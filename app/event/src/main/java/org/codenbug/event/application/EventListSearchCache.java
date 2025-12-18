package org.codenbug.event.application;

import org.codenbug.event.global.EventListResponse;
import org.codenbug.event.query.EventListProjection;

import java.util.List;

public interface EventListSearchCache<K,V> {

    void put(K cacheKey, V eventPage);

    V get(K key);

    boolean isCacheable(K cacheKey);

    boolean exist(K cacheKey);
}
