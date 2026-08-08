package com.pullwise.api.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuração do Redis para caching. Inativa em profile {@code test} —
 * tests de integração proveem um {@code ConcurrentMapCacheManager} via
 * {@code TestCacheConfig} para evitar dependência de Redis.
 */
@Configuration
@Profile("!test")
@EnableCaching
public class RedisConfig {

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("organizations", config.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("projects", config.entryTtl(Duration.ofHours(6)))
                .withCacheConfiguration("reviews", config.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("configurations", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("blast-radius", config.entryTtl(Duration.ofMinutes(10)))
                .transactionAware()
                .build();
    }
}
