package org.codenbug.cachecore.event.search;

public interface CacheClient<K, V> {

    void put(K cacheKey, V value);

    V get(K key);

    boolean exist(K cacheKey);
}
