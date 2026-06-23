package com.caixacombo.payment.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    @Primary
    @Profile("!redis")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products", "categories", "dashboard", "stone-status", "config");
    }
}
