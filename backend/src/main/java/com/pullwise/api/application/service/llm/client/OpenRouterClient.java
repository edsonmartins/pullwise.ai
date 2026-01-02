package com.pullwise.api.application.service.llm.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pullwise.api.domain.enums.LLMProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente para a API OpenRouter.
 *
 * <p>OpenRouter é um agregador de múltiplos modelos LLM que fornece:
 * - Acesso unificado a OpenAI, Anthropic, Google, Mistral, etc.
 * - Preços competitivos
 * - Roteamento automático de fallback
 * - API compatível com OpenAI
 *
 * @see <a href="https://openrouter.ai/docs">OpenRouter API Documentation</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${integrations.openrouter.api-key:}")
    private String apiKey;

    @Value("${integrations.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${integrations.openrouter.timeout:120}")
    private int timeoutSeconds;

    /**
     * Envia uma requisição de chat completion para o OpenRouter.
     *
     * @param request Configuração da requisição
     * @return Resposta com o conteúdo gerado e metadata
     */
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenRouter API key not configured");
        }

        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://pullwise.ai")
                .defaultHeader("X-Title", "Pullwise.ai")
                .build();

        try {
            ChatCompletionResponse response = client.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null && response.error != null) {
                log.error("OpenRouter API error: {} - {}", response.error.type, response.error.message);
                throw new RuntimeException("OpenRouter API error: " + response.error.message);
            }

            return response;

        } catch (Exception e) {
            log.error("Error calling OpenRouter API", e);
            throw new RuntimeException("Failed to call OpenRouter API: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna uma lista de modelos disponíveis no OpenRouter.
     */
    public List<ModelInfo> getAvailableModels() {
        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        try {
            ModelsResponse response = client.get()
                    .uri("/models")
                    .retrieve()
                    .bodyToMono(ModelsResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return response != null ? response.data : new ArrayList<>();

        } catch (Exception e) {
            log.error("Error fetching available models from OpenRouter", e);
            return new ArrayList<>();
        }
    }

    // ========== Request/Response DTOs ==========

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private List<String> stop;
        private Boolean stream;

        public ChatCompletionRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
            this.temperature = 0.7;
            this.maxTokens = 4096;
        }

        public static ChatCompletionRequest of(String model, String systemPrompt, String userPrompt) {
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(new Message("system", systemPrompt));
            }
            messages.add(new Message("user", userPrompt));
            return new ChatCompletionRequest(model, messages);
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class ChatCompletionResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
        private ErrorResponse error;

        public String getContent() {
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Message message = choices.get(0).message;
            return message != null ? message.content : null;
        }

        public Integer getTotalTokens() {
            return usage != null ? usage.totalTokens : null;
        }

        public Integer getPromptTokens() {
            return usage != null ? usage.promptTokens : null;
        }

        public Integer getCompletionTokens() {
            return usage != null ? usage.completionTokens : null;
        }
    }

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Data
    public static class ErrorResponse {
        private String type;
        private String message;
    }

    @Data
    public static class ModelsResponse {
        private String object;
        private List<ModelInfo> data;
    }

    @Data
    public static class ModelInfo {
        private String id;
        private String name;
        private String description;
        private Long contextLength;
        private Pricing pricing;
    }

    @Data
    public static class Pricing {
        @JsonProperty("prompt")
        private String promptPrice;

        @JsonProperty("completion")
        private String completionPrice;
    }
}
