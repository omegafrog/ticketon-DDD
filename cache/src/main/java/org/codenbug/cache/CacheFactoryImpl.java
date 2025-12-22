package org.codenbug.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.codenbug.cachecore.event.search.CacheClient;
import org.codenbug.cachecore.event.search.CacheFactory;

public class CacheFactoryImpl implements CacheFactory {

    @Override
    public <K, V> CacheClient<K, V> createCache(String cacheName, Duration ttl, long maxSize) {
        Cache<K, V> build = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttl.getSeconds(), TimeUnit.SECONDS)
            .recordStats()
            .build();

        return new CacheImpl<>(build);
    }
}
