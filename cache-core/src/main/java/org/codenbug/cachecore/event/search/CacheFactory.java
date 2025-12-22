package org.codenbug.cachecore.event.search;

import java.time.Duration;
import java.util.concurrent.Executor;

public interface CacheFactory {

    <K, V> CacheClient<K, V> createCache(String cacheName, Duration ttl, long maxSize,
        Executor executor);
}
