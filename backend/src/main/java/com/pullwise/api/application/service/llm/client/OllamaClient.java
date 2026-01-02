package com.pullwise.api.application.service.llm.client;

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
 * Cliente para a API Ollama (modelos locais).
 *
 * <p>Ollama permite executar modelos LLM localmente:
 * - Llama 2, Llama 3, Mistral, Gemma, etc.
 * - Sem custo por token
 * - Privacidade total (dados não saem da máquina)
 * - Latência pode ser maior dependendo do hardware
 *
 * @see <a href="https://github.com/ollama/ollama">Ollama GitHub</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${integrations.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${integrations.ollama.timeout:300}")
    private int timeoutSeconds;

    /**
     * Verifica se o Ollama está disponível.
     */
    public boolean isAvailable() {
        try {
            WebClient client = webClientBuilder.baseUrl(baseUrl).build();
            String tags = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return tags != null;
        } catch (Exception e) {
            log.debug("Ollama not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia uma requisição de chat completion para o Ollama.
     *
     * @param request Configuração da requisição
     * @return Resposta com o conteúdo gerado
     */
    public ChatResponse chat(ChatRequest request) {
        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            ChatResponse response = client.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null && response.error != null) {
                log.error("Ollama error: {}", response.error);
                throw new RuntimeException("Ollama error: " + response.error);
            }

            return response;

        } catch (Exception e) {
            log.error("Error calling Ollama API", e);
            throw new RuntimeException("Failed to call Ollama API: " + e.getMessage(), e);
        }
    }

    /**
     * Gera um embedding para o texto informado.
     *
     * @param model Modelo de embedding (ex: "nomic-embed-text")
     * @param text  Texto para gerar embedding
     * @return Vetor de embedding
     */
    public List<Double> generateEmbedding(String model, String text) {
        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        EmbeddingRequest request = new EmbeddingRequest(model, text);

        try {
            EmbeddingResponse response = client.post()
                    .uri("/api/embed")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return response != null ? response.embedding : new ArrayList<>();

        } catch (Exception e) {
            log.error("Error generating embedding with Ollama", e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Lista os modelos disponíveis no Ollama.
     */
    public List<ModelInfo> listModels() {
        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .build();

        try {
            TagsResponse response = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(TagsResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return response != null ? response.models : new ArrayList<>();

        } catch (Exception e) {
            log.error("Error listing Ollama models", e);
            return new ArrayList<>();
        }
    }

    // ========== Request/Response DTOs ==========

    @Data
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Boolean stream = false;

        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }

        public static ChatRequest of(String model, String systemPrompt, String userPrompt) {
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(new Message("system", systemPrompt));
            }
            messages.add(new Message("user", userPrompt));
            return new ChatRequest(model, messages);
        }
    }

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class ChatResponse {
        private String model;
        private String createdAt;
        private Message message;
        private String doneReason;
        private String error;

        public String getContent() {
            return message != null ? message.content : null;
        }
    }

    @Data
    public static class EmbeddingRequest {
        private String model;
        private String input;

        public EmbeddingRequest(String model, String input) {
            this.model = model;
            this.input = input;
        }
    }

    @Data
    public static class EmbeddingResponse {
        private String model;
        private List<Double> embedding;
    }

    @Data
    public static class TagsResponse {
        private List<ModelInfo> models;
    }

    @Data
    public static class ModelInfo {
        private String name;
        private String modifiedAt;
        private Long size;

        @JsonProperty("digest")
        private String digest;
    }
}
