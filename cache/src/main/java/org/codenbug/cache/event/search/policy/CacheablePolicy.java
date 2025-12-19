package org.codenbug.cache.event.search.policy;

public interface CacheablePolicy<T> {

    boolean support(Class<?> type);
    boolean isCacheable(T value);
}
