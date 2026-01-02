package com.pullwise.api.application.service.llm.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider para criar instâncias de ChatLanguageModel do LangChain4j.
 *
 * <p>Suporta múltiplos provedores LLM:
 * - OpenRouter (agregador)
 * - OpenAI
 * - Ollama (local)
 * - Anthropic (via OpenRouter)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "pullwise.llm",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LLMChatModelProvider {

    private final LLMChatModelProperties properties;

    /**
     * Cria uma instância de ChatLanguageModel baseado na configuração.
     *
     * @return ChatLanguageModel configurado
     * @throws IllegalStateException se a configuração for inválida
     */
    public ChatLanguageModel createChatModel() {
        if (!properties.isValid()) {
            throw new IllegalStateException("Invalid LLM configuration. Please check application.yml.");
        }

        log.info("Creating ChatLanguageModel: provider={}, model={}, baseUrl={}",
                properties.getProvider(),
                properties.getEffectiveModel(),
                properties.getEffectiveBaseUrl());

        return switch (properties.getProvider()) {
            case OPENROUTER -> createOpenRouterModel();
            case OLLAMA -> createOllamaModel();
            case OPENAI -> createOpenAiModel();
            case ANTHROPIC -> createAnthropicModel();
            case DEEPSEEK -> createDeepSeekModel();
            case GEMMA -> createGemmaModel();
        };
    }

    /**
     * Cria um ChatModel para um provedor e modelo específicos.
     *
     * @param provider Provedor LLM
     * @param modelId  ID do modelo
     * @return ChatLanguageModel configurado
     */
    public ChatLanguageModel createChatModel(LLMChatModelProperties.LLMProvider provider, String modelId) {
        log.info("Creating ChatLanguageModel for specific provider/model: {}/{}", provider, modelId);

        // Cria propriedades temporárias com overrides
        LLMChatModelProperties overrideProps = new LLMChatModelProperties();
        overrideProps.setEnabled(properties.getEnabled());
        overrideProps.setProvider(provider);
        overrideProps.setModel(modelId);
        overrideProps.setBaseUrl(properties.getBaseUrl());
        overrideProps.setApiKey(properties.getApiKey());
        overrideProps.setTemperature(properties.getTemperature());
        overrideProps.setMaxTokens(properties.getMaxTokens());
        overrideProps.setTimeout(properties.getTimeout());

        // Usa switch com overrideProps
        return switch (provider) {
            case OPENROUTER -> createOpenRouterModel(overrideProps);
            case OLLAMA -> createOllamaModel(overrideProps);
            case OPENAI -> createOpenAiModel(overrideProps);
            case ANTHROPIC -> createAnthropicModel(overrideProps);
            case DEEPSEEK -> createDeepSeekModel(overrideProps);
            case GEMMA -> createGemmaModel(overrideProps);
        };
    }

    // ========== Private Methods ==========

    /**
     * Cria modelo OpenRouter (usa API OpenAI-compatible).
     */
    private ChatLanguageModel createOpenRouterModel() {
        return createOpenRouterModel(properties);
    }

    private ChatLanguageModel createOpenRouterModel(LLMChatModelProperties props) {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("HTTP-Referer", "https://pullwise.ai");
        customHeaders.put("X-Title", "Pullwise.ai");

        return OpenAiChatModel.builder()
                .baseUrl(props.getEffectiveBaseUrl())
                .apiKey(props.getApiKey())
                .modelName(props.getEffectiveModel())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(props.getTimeout())
                .maxRetries(props.getMaxRetries())
                .logRequests(props.getLogRequests())
                .logResponses(props.getLogResponses())
                .build();
    }

    /**
     * Cria modelo Ollama (local).
     */
    private ChatLanguageModel createOllamaModel() {
        return createOllamaModel(properties);
    }

    private ChatLanguageModel createOllamaModel(LLMChatModelProperties props) {
        return OllamaChatModel.builder()
                .baseUrl(props.getEffectiveBaseUrl())
                .modelName(props.getEffectiveModel())
                .temperature(props.getTemperature())
                .timeout(props.getTimeout())
                .build();
    }

    /**
     * Cria modelo OpenAI.
     */
    private ChatLanguageModel createOpenAiModel() {
        return createOpenAiModel(properties);
    }

    private ChatLanguageModel createOpenAiModel(LLMChatModelProperties props) {
        return OpenAiChatModel.builder()
                .apiKey(props.getApiKey())
                .modelName(props.getEffectiveModel())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(props.getTimeout())
                .maxRetries(props.getMaxRetries())
                .logRequests(props.getLogRequests())
                .logResponses(props.getLogResponses())
                .build();
    }

    /**
     * Cria modelo Anthropic (via OpenRouter).
     */
    private ChatLanguageModel createAnthropicModel() {
        return createOpenRouterModel();
    }

    private ChatLanguageModel createAnthropicModel(LLMChatModelProperties props) {
        return createOpenRouterModel(props);
    }

    /**
     * Cria modelo DeepSeek.
     */
    private ChatLanguageModel createDeepSeekModel() {
        return createDeepSeekModel(properties);
    }

    private ChatLanguageModel createDeepSeekModel(LLMChatModelProperties props) {
        // DeepSeek usa API OpenAI-compatible
        return OpenAiChatModel.builder()
                .baseUrl(props.getEffectiveBaseUrl())
                .apiKey(props.getApiKey())
                .modelName(props.getEffectiveModel())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(props.getTimeout())
                .build();
    }

    /**
     * Cria modelo Gemma (via Ollama).
     */
    private ChatLanguageModel createGemmaModel() {
        return createOllamaModel(properties);
    }

    private ChatLanguageModel createGemmaModel(LLMChatModelProperties props) {
        return createOllamaModel(props);
    }
}
