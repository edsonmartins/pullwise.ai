package com.pullwise.api.application.service.llm.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Propriedades de configuração para o ChatModel LLM.
 *
 * <p>Lê configuração de application.yml:
 * <pre>
 * pullwise:
 *   llm:
 *     enabled: true
 *     provider: OPENROUTER
 *     model: anthropic/claude-3.5-sonnet
 *     base-url: https://openrouter.ai/api/v1
 *     api-key: ${OPENROUTER_API_KEY}
 *     temperature: 0.7
 *     max-tokens: 4096
 *     timeout: 120s
 *     max-retries: 3
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "pullwise.llm")
public class LLMChatModelProperties {

    /**
     * Habilita/desabilita o serviço LLM.
     */
    private Boolean enabled = true;

    /**
     * Provedor LLM principal.
     */
    private LLMProvider provider = LLMProvider.OPENROUTER;

    /**
     * Nome do modelo a ser usado.
     */
    private String model = "anthropic/claude-3.5-sonnet";

    /**
     * URL base da API (para provedores customizados).
     */
    private String baseUrl;

    /**
     * API key para autenticação.
     */
    private String apiKey;

    /**
     * Temperatura para geração (0.0 - 2.0).
     */
    private Double temperature = 0.7;

    /**
     * Número máximo de tokens na resposta.
     */
    private Integer maxTokens = 4096;

    /**
     * Timeout para requisições.
     */
    private Duration timeout = Duration.ofSeconds(120);

    /**
     * Número máximo de retries em caso de falha.
     */
    private Integer maxRetries = 3;

    /**
     * Logar requisições da API.
     */
    private Boolean logRequests = false;

    /**
     * Logar respostas da API.
     */
    private Boolean logResponses = false;

    /**
     * Configuração de modelos por tarefa (para router).
     */
    private Map<String, ModelConfig> models = new HashMap<>();

    /**
     * Configuração de provedores alternativos.
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /**
     * Habilita modo "thinking" (para modelos DeepSeek R1).
     */
    private Boolean enableThinking = false;

    /**
     * Configuração Cloudflare Access (opcional).
     */
    private CloudflareConfig cloudflare;

    /**
     * Retorna o modelo efetivo a ser usado.
     */
    public String getEffectiveModel() {
        return model != null ? model : "gpt-3.5-turbo";
    }

    /**
     * Retorna a URL base efetiva.
     */
    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        return switch (provider) {
            case OPENROUTER -> "https://openrouter.ai/api/v1";
            case OLLAMA -> "http://localhost:11434";
            case OPENAI -> "https://api.openai.com/v1";
            case ANTHROPIC -> "https://api.anthropic.com/v1";
            case DEEPSEEK -> "https://api.deepseek.com/v1";
            case GEMMA -> "http://localhost:11434";  // Gemma usa Ollama localmente
        };
    }

    /**
     * Verifica se a configuração é válida.
     */
    public boolean isValid() {
        if (!enabled) {
            return true;  // Desabilitado é válido
        }

        if (provider == null) {
            return false;
        }

        if (provider == LLMProvider.OPENROUTER || provider == LLMProvider.OPENAI) {
            return apiKey != null && !apiKey.isEmpty();
        }

        return true;
    }

    /**
     * Provedores LLM suportados.
     */
    public enum LLMProvider {
        OPENROUTER,
        OLLAMA,
        OPENAI,
        ANTHROPIC,
        DEEPSEEK,
        GEMMA
    }

    /**
     * Configuração de um modelo específico.
     */
    @Data
    public static class ModelConfig {
        private String provider;
        private String modelId;
        private Double temperature;
        private Integer maxTokens;
        private Double costPer1kTokens;
        private java.util.List<String> useCases;
    }

    /**
     * Configuração de um provedor.
     */
    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
    }

    /**
     * Configuração Cloudflare Access.
     */
    @Data
    public static class CloudflareConfig {
        private Boolean enabled = false;
        private String clientId;
        private String clientSecret;

        public boolean isConfigured() {
            return enabled != null && enabled
                    && clientId != null && !clientId.isEmpty()
                    && clientSecret != null && !clientSecret.isEmpty();
        }
    }
}
