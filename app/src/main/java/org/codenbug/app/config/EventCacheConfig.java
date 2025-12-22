package org.codenbug.app.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.codenbug.cache.CacheFactoryImpl;
import org.codenbug.cache.CacheKeyVersionManagerImpl;
import org.codenbug.cachecore.event.search.CacheClient;
import org.codenbug.cachecore.event.search.CacheFactory;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.codenbug.event.application.cache.EventListSearchCacheValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventCacheConfig {

    @Bean
    public CacheClient<EventListSearchCacheKey, EventListSearchCacheValue> searchCacheClient(
        CacheFactory cacheFactory) {
        return cacheFactory.createCache("eventSearch", Duration.of(5, ChronoUnit.MINUTES), 10_000);
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
