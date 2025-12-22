package org.codenbug.cachecore.event.search;

import java.util.function.Supplier;

public interface CacheClient<K, V> {

    void put(K cacheKey, V value);

    V get(K key);

    V get(K key, Supplier<V> loader);

    boolean exist(K cacheKey);
}
