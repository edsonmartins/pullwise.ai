package com.pullwise.api.config;

import com.pullwise.api.application.service.graph.blast.BlastRadiusService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Provê um {@link CacheManager} em memória para testes de integração que
 * não querem subir Redis. Substitui o {@link RedisConfig} (desativado em
 * profile {@code test}).
 */
@TestConfiguration
@Profile("test")
@EnableCaching
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(BlastRadiusService.CACHE_NAME);
    }
}
