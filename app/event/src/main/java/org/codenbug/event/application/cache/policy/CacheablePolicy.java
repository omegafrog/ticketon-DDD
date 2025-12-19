package org.codenbug.event.application.cache.policy;

public interface CacheablePolicy<T> {

    boolean support(Class<?> type);

    boolean isCacheable(T value);
}
