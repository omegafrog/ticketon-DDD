package org.codenbug.cachecore.event.search;

import java.time.Duration;

public interface CacheFactory {

    <K, V> CacheClient<K, V> createCache(String cacheName, Duration ttl, long maxSize);
}
