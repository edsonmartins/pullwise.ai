package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Provedores de LLM suportados pelo Multi-Model Router.
 *
 * <p>Cada provedor tem características específicas de:
 * - API endpoint
 * - Formato de requisição/resposta
 * - Modelo de cobrança
 * - Modelos disponíveis
 */
@Getter
public enum LLMProvider {

    /**
     * OpenRouter - Agregador de múltiplos modelos LLM.
     * Fornece acesso unificado a: OpenAI, Anthropic, Google, Mistral, etc.
     */
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        "Bearer {key}",
        true
    ),

    /**
     * Anthropic Claude (acesso direto, sem OpenRouter).
     */
    ANTHROPIC(
        "Anthropic",
        "https://api.anthropic.com/v1",
        "Bearer {key}",
        true
    ),

    /**
     * OpenAI GPT (acesso direto, sem OpenRouter).
     */
    OPENAI(
        "OpenAI",
        "https://api.openai.com/v1",
        "Bearer {key}",
        true
    ),

    /**
     * Ollama - Modelos locais open source.
     * Executa modelos localmente: Llama, Gemma, Mistral, etc.
     */
    OLLAMA(
        "Ollama",
        "http://localhost:11434",
        null,
        false
    ),

    /**
     * AWS Bedrock - Modelos enterprise da Amazon.
     */
    BEDROCK(
        "AWS Bedrock",
        "https://bedrock-runtime.{region}.amazonaws.com",
        "AWS4-HMAC-SHA256",
        true
    ),

    /**
     * Azure OpenAI - Enterprise OpenAI via Azure.
     */
    AZURE_OPENAI(
        "Azure OpenAI",
        "https://{resource}.openai.azure.com",
        "Bearer {key}",
        true
    );

    private final String displayName;
    private final String baseUrl;
    private final String authFormat;  // null para auth não necessário
    private final boolean requiresApiKey;

    LLMProvider(String displayName, String baseUrl, String authFormat, boolean requiresApiKey) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.authFormat = authFormat;
        this.requiresApiKey = requiresApiKey;
    }

    /**
     * Retorna true se este provedor é um modelo local (sem custo por token).
     */
    public boolean isLocal() {
        return this == OLLAMA;
    }

    /**
     * Retorna true se este provedor é um agregador de múltiplos modelos.
     */
    public boolean isAggregator() {
        return this == OPENROUTER;
    }

    /**
     * Retorna a configuração de ambiente necessária para este provedor.
     */
    public String getEnvKeyName() {
        return switch (this) {
            case OPENROUTER -> "OPENROUTER_API_KEY";
            case ANTHROPIC -> "ANTHROPIC_API_KEY";
            case OPENAI -> "OPENAI_API_KEY";
            case OLLAMA -> "OLLAMA_URL";
            case BEDROCK -> "AWS_ACCESS_KEY_ID,AWS_SECRET_ACCESS_KEY";
            case AZURE_OPENAI -> "AZURE_OPENAI_API_KEY";
        };
    }
}
