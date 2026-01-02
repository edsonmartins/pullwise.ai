package com.pullwise.api.application.service.llm.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.llm.client.OllamaClient;
import com.pullwise.api.application.service.llm.client.OpenRouterClient;
import com.pullwise.api.application.service.llm.model.LLMModelConfig;
import com.pullwise.api.domain.model.LLMRoutingDecision;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.repository.LLMRoutingDecisionRepository;
import com.pullwise.api.domain.enums.ReviewTaskType;
import com.pullwise.api.domain.enums.LLMProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Router inteligente que seleciona o modelo LLM ideal para cada tipo de tarefa.
 *
 * <p>Estratégias de roteamento:
 * <ul>
 *   <li><b>COST_OPTIMIZED</b>: Prioriza menor custo, usa modelos locais quando possível</li>
 *   <li><b>QUALITY_FIRST</b>: Prioriza máxima qualidade, sempre usa o melhor modelo</li>
 *   <li><b>BALANCED</b>: Balanceia custo e qualidade baseado na tarefa</li>
 * </ul>
 *
 * <p>O router também:
 * - Tracking de custos por chamada
 * - Análise de latência
 * - Decisões de fallback em caso de erro
 * - Analytics de uso por modelo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiModelLLMRouter {

    private final LLMModelConfig config;
    private final OpenRouterClient openRouterClient;
    private final OllamaClient ollamaClient;
    private final LLMRoutingDecisionRepository decisionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Executa uma tarefa de LLM usando o modelo apropriado.
     *
     * @param task     Tipo da tarefa
     * @param systemPrompt Prompt do sistema
     * @param userPrompt   Prompt do usuário
     * @return Resposta gerada pelo modelo
     */
    public LLMResponse execute(ReviewTaskType task, String systemPrompt, String userPrompt) {
        return execute(task, systemPrompt, userPrompt, null);
    }

    /**
     * Executa uma tarefa de LLM associando a um review específico.
     */
    public LLMResponse execute(ReviewTaskType task, String systemPrompt, String userPrompt, Review review) {
        long startTime = System.currentTimeMillis();

        // Seleciona o modelo apropriado
        ModelSelection selection = selectModel(task);

        log.debug("Selected model {} for task {} (strategy: {})",
                selection.modelId(), task, config.getRouter().getStrategy());

        LLMRoutingDecision decision = recordDecisionStart(task, selection, review);

        try {
            // Executa a chamada ao modelo
            String content = invokeModel(selection, systemPrompt, userPrompt);

            // Finaliza o registro
            long latency = System.currentTimeMillis() - startTime;
            recordDecisionSuccess(decision, latency, content, selection);

            return new LLMResponse(
                    content,
                    selection.modelId(),
                    selection.provider(),
                    latency,
                    decision.getCostUsd()
            );

        } catch (Exception e) {
            log.error("Error executing LLM task {} with model {}", task, selection.modelId(), e);

            // Tenta fallback se disponível
            if (selection.isFallback()) {
                throw new RuntimeException("Failed to execute LLM task and fallback also failed", e);
            }

            return executeFallback(task, systemPrompt, userPrompt, review, startTime, decision, e);
        }
    }

    /**
     * Executa uma tarefa de LLM com histórico de conversa.
     */
    public LLMResponse execute(ReviewTaskType task, List<Message> messages) {
        return execute(task, messages, null);
    }

    /**
     * Executa uma tarefa de LLM com histórico de conversa e review associado.
     */
    public LLMResponse execute(ReviewTaskType task, List<Message> messages, Review review) {
        long startTime = System.currentTimeMillis();

        ModelSelection selection = selectModel(task);

        LLMRoutingDecision decision = recordDecisionStart(task, selection, review);

        try {
            String content = invokeModelWithMessages(selection, messages);

            long latency = System.currentTimeMillis() - startTime;
            recordDecisionSuccess(decision, latency, content, selection);

            return new LLMResponse(
                    content,
                    selection.modelId(),
                    selection.provider(),
                    latency,
                    decision.getCostUsd()
            );

        } catch (Exception e) {
            log.error("Error executing LLM task {} with model {}", task, selection.modelId(), e);

            if (selection.isFallback()) {
                throw new RuntimeException("Failed to execute LLM task and fallback also failed", e);
            }

            return executeFallback(task, messages, review, startTime, decision, e);
        }
    }

    // ========== Private Methods ==========

    /**
     * Seleciona o modelo apropriado baseado na estratégia configurada.
     */
    private ModelSelection selectModel(ReviewTaskType task) {
        return switch (config.getRouter().getStrategy()) {
            case COST_OPTIMIZED -> selectCostOptimized(task);
            case QUALITY_FIRST -> selectQualityFirst(task);
            case BALANCED -> selectBalanced(task);
        };
    }

    /**
     * Seleção otimizada para custo - tenta usar modelos locais primeiro.
     */
    private ModelSelection selectCostOptimized(ReviewTaskType task) {
        // Tenta modelo local primeiro
        if (task.canUseLocalModel() && ollamaClient.isAvailable()) {
            Optional<LLMModelConfig.ModelConfig> localModel = config.getModels().values().stream()
                    .filter(m -> m.isLocal() && m.supportsTask(task))
                    .findFirst();

            if (localModel.isPresent()) {
                LLMModelConfig.ModelConfig model = localModel.get();
                return new ModelSelection(model.getModelId(), model.getProvider(), false);
            }
        }

        // Usa o mais econômico disponível
        return config.getCheapestModelForTask(task)
                .map(m -> new ModelSelection(m.getModelId(), m.getProvider(), false))
                .orElseGet(() -> getDefaultSelection());
    }

    /**
     * Seleção focada em qualidade - sempre usa o melhor modelo.
     */
    private ModelSelection selectQualityFirst(ReviewTaskType task) {
        return config.getBestModelForTask(task)
                .map(m -> new ModelSelection(m.getModelId(), m.getProvider(), false))
                .orElseGet(() -> getDefaultSelection());
    }

    /**
     * Seleção balanceada - baseada na prioridade da tarefa.
     */
    private ModelSelection selectBalanced(ReviewTaskType task) {
        if (task.requiresHighCapability()) {
            return selectQualityFirst(task);
        } else if (task.canUseLocalModel() && ollamaClient.isAvailable()) {
            return selectCostOptimized(task);
        } else {
            return config.getCheapestModelForTask(task)
                    .map(m -> new ModelSelection(m.getModelId(), m.getProvider(), false))
                    .orElseGet(() -> getDefaultSelection());
        }
    }

    /**
     * Retorna a seleção padrão configurada.
     */
    private ModelSelection getDefaultSelection() {
        return config.getDefaultModel()
                .map(m -> new ModelSelection(m.getModelId(), m.getProvider(), false))
                .orElseThrow(() -> new IllegalStateException("No default model configured"));
    }

    /**
     * Invoca o modelo selecionado.
     */
    private String invokeModel(ModelSelection selection, String systemPrompt, String userPrompt) {
        return switch (selection.provider()) {
            case OLLAMA -> {
                OllamaClient.ChatRequest request = OllamaClient.ChatRequest.of(
                        selection.modelId().split(":")[0], // Remove tag se presente
                        systemPrompt,
                        userPrompt
                );
                OllamaClient.ChatResponse response = ollamaClient.chat(request);
                yield response.getContent();
            }
            case OPENROUTER, ANTHROPIC, OPENAI -> {
                OpenRouterClient.ChatCompletionRequest request =
                        OpenRouterClient.ChatCompletionRequest.of(selection.modelId(), systemPrompt, userPrompt);
                OpenRouterClient.ChatCompletionResponse response = openRouterClient.chatCompletion(request);
                yield response.getContent();
            }
            default -> throw new IllegalArgumentException("Unsupported provider: " + selection.provider());
        };
    }

    /**
     * Invoca o modelo com lista de mensagens.
     */
    private String invokeModelWithMessages(ModelSelection selection, List<Message> messages) {
        // Simplificado - converte mensagens para prompts
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder systemPrompt = new StringBuilder();
        StringBuilder userPrompt = new StringBuilder();

        for (Message msg : messages) {
            if ("system".equals(msg.role())) {
                systemPrompt.append(msg.content()).append("\n");
            } else {
                userPrompt.append(msg.content()).append("\n");
            }
        }

        return invokeModel(selection,
                systemPrompt.toString().trim(),
                userPrompt.toString().trim());
    }

    /**
     * Registra o início de uma decisão de roteamento.
     */
    private LLMRoutingDecision recordDecisionStart(ReviewTaskType task, ModelSelection selection, Review review) {
        LLMRoutingDecision decision = LLMRoutingDecision.builder()
                .review(review)
                .taskType(task)
                .selectedModel(selection.modelId())
                .provider(com.pullwise.api.domain.model.LLMRoutingDecision.LLMProvider.valueOf(selection.provider().name()))
                .reasoning(buildReasoningJson(task, selection))
                .createdAt(LocalDateTime.now())
                .build();

        return decisionRepository.save(decision);
    }

    /**
     * Registra o sucesso de uma decisão de roteamento.
     */
    private void recordDecisionSuccess(LLMRoutingDecision decision, long latencyMs,
                                       String content, ModelSelection selection) {
        // Estima tokens (simplificado)
        int inputTokens = estimateTokens(decision.getReasoning());
        int outputTokens = estimateTokens(content);

        // Calcula custo
        double cost = config.findModelById(selection.modelId())
                .map(m -> m.estimateCost(inputTokens, outputTokens))
                .orElse(0.0);

        decision.setInputTokens(inputTokens);
        decision.setOutputTokens(outputTokens);
        decision.setCostUsd(BigDecimal.valueOf(cost));
        decision.setLatencyMs((int) latencyMs);

        decisionRepository.save(decision);
    }

    /**
     * Executa fallback em caso de erro.
     */
    private LLMResponse executeFallback(ReviewTaskType task, String systemPrompt, String userPrompt,
                                       Review review, long startTime, LLMRoutingDecision originalDecision,
                                       Exception originalError) {
        log.warn("Attempting fallback for task {}", task);

        return config.getFallbackModel()
                .map(fallback -> {
                    ModelSelection fallbackSelection = new ModelSelection(
                            fallback.getModelId(),
                            fallback.getProvider(),
                            true
                    );

                    try {
                        String content = invokeModel(fallbackSelection, systemPrompt, userPrompt);
                        long latency = System.currentTimeMillis() - startTime;

                        // Atualiza a decisão original para registrar o fallback
                        originalDecision.setReasoning(originalDecision.getReasoning() +
                                "\nFALLBACK: " + originalError.getMessage());
                        recordDecisionSuccess(originalDecision, latency, content, fallbackSelection);

                        return new LLMResponse(
                                content,
                                fallbackSelection.modelId(),
                                fallbackSelection.provider(),
                                latency,
                                BigDecimal.ZERO
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Fallback also failed", e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("No fallback model configured", originalError));
    }

    /**
     * Executa fallback com lista de mensagens.
     */
    private LLMResponse executeFallback(ReviewTaskType task, List<Message> messages,
                                       Review review, long startTime, LLMRoutingDecision originalDecision,
                                       Exception originalError) {
        // Simplificado - converte mensagens para prompts
        if (messages.isEmpty()) {
            throw new RuntimeException("No messages to process", originalError);
        }

        StringBuilder systemPrompt = new StringBuilder();
        StringBuilder userPrompt = new StringBuilder();

        for (Message msg : messages) {
            if ("system".equals(msg.role())) {
                systemPrompt.append(msg.content()).append("\n");
            } else {
                userPrompt.append(msg.content()).append("\n");
            }
        }

        return executeFallback(task, systemPrompt.toString(), userPrompt.toString(),
                review, startTime, originalDecision, originalError);
    }

    /**
     * Constrói JSON com o reasoning da seleção.
     */
    private String buildReasoningJson(ReviewTaskType task, ModelSelection selection) {
        try {
            return objectMapper.writeValueAsString(new Reasoning(
                    task,
                    config.getRouter().getStrategy(),
                    selection.modelId(),
                    selection.provider(),
                    task.getQualityPriority(),
                    task.canUseLocalModel()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Estima o número de tokens (simplificado: ~4 caracteres por token).
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        return (text.length() / 4) + 1;
    }

    // ========== DTOs ==========

    public record ModelSelection(
            String modelId,
            LLMProvider provider,
            boolean isFallback
    ) {}

    public record Message(
            String role,
            String content
    ) {}

    public record LLMResponse(
            String content,
            String modelId,
            LLMProvider provider,
            long latencyMs,
            BigDecimal cost
    ) {}

    private record Reasoning(
            ReviewTaskType taskType,
            LLMModelConfig.Strategy strategy,
            String selectedModel,
            LLMProvider provider,
            int qualityPriority,
            boolean canUseLocal
    ) {}
}
