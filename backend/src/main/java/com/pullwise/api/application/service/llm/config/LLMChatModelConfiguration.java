package com.pullwise.api.application.service.llm.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Configuração Spring para o ChatModel LLM.
 *
 * <p>Registra o ChatLanguageModel como bean do Spring e configura o cache.
 */
@Slf4j
@Configuration
@EnableCaching
@EnableConfigurationProperties(LLMChatModelProperties.class)
@ConditionalOnProperty(
        prefix = "pullwise.llm",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LLMChatModelConfiguration {

    private final LLMChatModelProvider provider;
    private final LLMChatModelProperties properties;

    public LLMChatModelConfiguration(LLMChatModelProvider provider,
                                      LLMChatModelProperties properties) {
        this.provider = provider;
        this.properties = properties;
    }

    @PostConstruct
    public void logConfiguration() {
        log.info("=== PULLWISE LLM CONFIGURATION ===");
        log.info("Enabled: {}", properties.getEnabled());
        log.info("Provider: {}", properties.getProvider());
        log.info("Model: {}", properties.getEffectiveModel());
        log.info("Base URL: {}", properties.getEffectiveBaseUrl());
        log.info("Temperature: {}", properties.getTemperature());
        log.info("Max Tokens: {}", properties.getMaxTokens());
        log.info("Timeout: {}", properties.getTimeout());
        log.info("=================================");
    }

    /**
     * RestTemplate para requisições LLM.
     */
    @Bean
    @Primary
    public RestTemplate llmRestTemplate() {
        org.springframework.boot.web.client.RestTemplateBuilder builder =
                new org.springframework.boot.web.client.RestTemplateBuilder();

        Duration timeout = properties.getTimeout() != null ? properties.getTimeout() : Duration.ofSeconds(120);

        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    /**
     * ChatLanguageModel primário para a aplicação.
     */
    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        log.info("Initializing ChatLanguageModel");

        ChatLanguageModel model = provider.createChatModel();
        log.info("ChatLanguageModel initialized successfully");

        return model;
    }
}
