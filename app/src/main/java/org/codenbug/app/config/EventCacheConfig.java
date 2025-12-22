package org.codenbug.app.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.codenbug.cache.CacheFactoryImpl;
import org.codenbug.cache.CacheKeyVersionManagerImpl;
import org.codenbug.cachecore.event.search.CacheClient;
import org.codenbug.cachecore.event.search.CacheFactory;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.codenbug.event.application.cache.EventListSearchCacheValue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventCacheConfig {

    private static final long TTL = 1000 * 60;

    @Bean
    public CacheClient<EventListSearchCacheKey, EventListSearchCacheValue> searchCacheClient(
        CacheFactory cacheFactory, @Qualifier("cacheRefreshExecutor") Executor executor) {
        return cacheFactory.createCache("eventSearch", Duration.of(TTL, ChronoUnit.MILLIS), 10_000,
            executor);
    }

    @Bean(name = "cacheRefreshExecutor")
    public Executor cacheRefreshExecutor() {
        return Executors.newFixedThreadPool(8);
    }

    @Bean
    public CacheFactory cacheFactory() {
        return new CacheFactoryImpl();
    }

    @Bean
    public CacheKeyVersionManager cacheKeyVersionManager() {
        return new CacheKeyVersionManagerImpl();
    }
}
