package org.codenbug.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.codenbug.cachecore.event.search.CacheClient;
import org.springframework.stereotype.Component;

@Component
public class CacheImpl<K, V> implements CacheClient<K, V> {

    private final Cache cache;

    public CacheImpl(Cache cache) {
        this.cache = cache;
    }


    @Override
    public void put(K cacheKey, V eventPage) {
        cache.put(cacheKey, eventPage);
    }

    @Override
    public V get(K key) {
        V result = (V) cache.getIfPresent(key);
        return result;
    }

    @Override
    public boolean exist(K cacheKey) {
        return cache.getIfPresent(cacheKey) != null;
    }

}
