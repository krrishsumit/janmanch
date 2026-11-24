package com.janmanch.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * If Redis is unavailable, or holds an entry written by an older version of
 * the code, log it and load from the database instead of failing the request.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
