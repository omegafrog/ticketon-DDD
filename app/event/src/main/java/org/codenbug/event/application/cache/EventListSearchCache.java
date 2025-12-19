package org.codenbug.event.application.cache;

public interface EventListSearchCache<K, V> {

    void put(K cacheKey, V eventPage);

    V get(K key);

    boolean isCacheable(K cacheKey);

    boolean exist(K cacheKey);
}
