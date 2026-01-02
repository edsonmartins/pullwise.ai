package com.pullwise.api.application.service.llm.model;

import com.pullwise.api.domain.enums.LLMProvider;
import com.pullwise.api.domain.enums.ReviewTaskType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * Configuração dos modelos LLM suportados pelo Multi-Model Router.
 *
 * <p>Lê as configurações de application.yml:
 * <pre>
 * llm:
 *   router:
 *     strategy: cost-optimized
 *     default-model: anthropic/claude-3.5-sonnet
 *   models:
 *     claude-3.5-sonnet:
 *       provider: openrouter
 *       model-id: anthropic/claude-3.5-sonnet
 *       max-tokens: 8192
 *       cost-per-1k-tokens: 0.003
 *       use-cases: [ARCHITECTURE_REVIEW, SECURITY_ANALYSIS]
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMModelConfig {

    private RouterConfig router = new RouterConfig();
    private Map<String, ModelConfig> models = new HashMap<>();
    private CostTrackingConfig costTracking = new CostTrackingConfig();

    @Data
    public static class RouterConfig {
        private boolean enabled = true;
        private Strategy strategy = Strategy.COST_OPTIMIZED;
        private String defaultModel = "anthropic/claude-3.5-sonnet";
        private String fallbackModel = "google/gemma-3-4b-it:free";
    }

    @Data
    public static class ModelConfig {
        private LLMProvider provider;
        private String modelId;
        private int maxTokens = 4096;
        private double costPer1kTokens = 0.001;
        private List<ReviewTaskType> useCases = new ArrayList<>();

        /**
         * Retorna true se este modelo é adequado para a tarefa especificada.
         */
        public boolean supportsTask(ReviewTaskType taskType) {
            return useCases.isEmpty() || useCases.contains(taskType);
        }

        /**
         * Retorna true se este é um modelo local (sem custo).
         */
        public boolean isLocal() {
            return provider == LLMProvider.OLLAMA;
        }

        /**
         * Calcula o custo estimado para uma quantidade de tokens.
         */
        public double estimateCost(int inputTokens, int outputTokens) {
            if (isLocal()) {
                return 0.0;
            }
            int totalTokens = inputTokens + outputTokens;
            return (totalTokens / 1000.0) * costPer1kTokens;
        }
    }

    @Data
    public static class CostTrackingConfig {
        private boolean enabled = true;
        private double dailyBudget = 50.0;
        private double alertThreshold = 0.8;  // 80%
    }

    public enum Strategy {
        /**
         * Prioriza menor custo, usa modelos locais quando possível.
         */
        COST_OPTIMIZED,

        /**
         * Prioriza máxima qualidade, sempre usa o melhor modelo.
         */
        QUALITY_FIRST,

        /**
         * Balanceia custo e qualidade baseado na tarefa.
         */
        BALANCED
    }

    /**
     * Busca a configuração de um modelo pelo seu ID.
     */
    public Optional<ModelConfig> findModelById(String modelId) {
        return models.values().stream()
                .filter(m -> modelId.equals(m.getModelId()))
                .findFirst();
    }

    /**
     * Retorna todos os modelos que suportam uma determinada tarefa.
     */
    public List<ModelConfig> getModelsForTask(ReviewTaskType taskType) {
        return models.values().stream()
                .filter(m -> m.supportsTask(taskType))
                .toList();
    }

    /**
     * Retorna o modelo padrão configurado.
     */
    public Optional<ModelConfig> getDefaultModel() {
        return findModelById(router.getDefaultModel());
    }

    /**
     * Retorna o modelo de fallback configurado.
     */
    public Optional<ModelConfig> getFallbackModel() {
        return findModelById(router.getFallbackModel());
    }

    /**
     * Retorna o modelo mais econômico para uma tarefa.
     */
    public Optional<ModelConfig> getCheapestModelForTask(ReviewTaskType taskType) {
        return models.values().stream()
                .filter(m -> m.supportsTask(taskType))
                .min(Comparator.comparingDouble(ModelConfig::getCostPer1kTokens));
    }

    /**
     * Retorna o modelo de maior qualidade para uma tarefa.
     * Considera prioridade de qualidade da tarefa.
     */
    public Optional<ModelConfig> getBestModelForTask(ReviewTaskType taskType) {
        if (taskType.requiresHighCapability()) {
            // Para tarefas críticas, prioriza modelos mais caros (geralmente melhores)
            return models.values().stream()
                    .filter(m -> m.supportsTask(taskType))
                    .filter(m -> !m.isLocal())  // evita modelos locais para tarefas críticas
                    .max(Comparator.comparingDouble(ModelConfig::getCostPer1kTokens));
        } else {
            // Para tarefas simples, pode usar modelos locais
            return getCheapestModelForTask(taskType);
        }
    }
}
