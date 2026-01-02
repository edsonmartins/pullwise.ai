package com.pullwise.api.application.service.llm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuração de cache para ChatModels LLM.
 *
 * <p>Usa Caffeine para cache em memória das instâncias de ChatModel.
 */
@Slf4j
@Configuration
@EnableCaching
public class ChatModelCacheConfig {

    /**
     * Nome do cache para ChatModels.
     */
    public static final String CHAT_MODEL_CACHE = "chatModels";

    /**
     * Cache Manager para ChatModels.
     */
    @Bean
    public CacheManager chatModelCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CHAT_MODEL_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }
}
