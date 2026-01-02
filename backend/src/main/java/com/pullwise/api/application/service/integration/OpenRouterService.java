package com.pullwise.api.application.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Serviço de integração com OpenRouter (agregador de LLMs).
 */
@Slf4j
@Service
public class OpenRouterService {

    @Value("${integrations.openrouter.api-key}")
    private String apiKey;

    @Value("${integrations.openrouter.api-url:https://openrouter.ai/api/v1}")
    private String apiUrl;

    @Value("${integrations.openrouter.model:anthropic/claude-3-haiku}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Analisa código usando OpenRouter API.
     */
    public String analyzeWithOpenRouter(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenRouter API key not configured");
            return "";
        }

        String url = apiUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "https://pullwise.ai");
        headers.set("X-Title", "Pullwise");

        ChatRequest request = new ChatRequest(
                model,
                List.of(new Message("user", prompt)),
                0.2,
                4096
        );

        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        try {
            ChatResponse response = restTemplate.postForObject(url, entity, ChatResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            }

            return "";

        } catch (Exception e) {
            log.error("OpenRouter API error", e);
            return "";
        }
    }

    /**
     * Analisa código usando Ollama (local).
     */
    public String analyzeWithOllama(String prompt) {
        String url = "http://localhost:11434/api/generate";

        OllamaRequest request = new OllamaRequest("llama3.2", prompt, false);

        try {
            OllamaResponse response = restTemplate.postForObject(url, request, OllamaResponse.class);

            if (response != null) {
                return response.response();
            }

            return "";

        } catch (Exception e) {
            log.error("Ollama API error", e);
            return "";
        }
    }

    public record ChatRequest(
            String model,
            List<Message> messages,
            double temperature,
            int max_tokens
    ) {}

    public record Message(
            String role,
            String content
    ) {}

    public record ChatResponse(
            List<Choice> choices
    ) {}

    public record Choice(
            Message message
    ) {}

    public record OllamaRequest(
            String model,
            String prompt,
            boolean stream
    ) {}

    public record OllamaResponse(
            String response,
            boolean done
    ) {}
}
