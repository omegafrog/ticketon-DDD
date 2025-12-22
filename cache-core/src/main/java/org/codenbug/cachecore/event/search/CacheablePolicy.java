package org.codenbug.cachecore.event.search;

public interface CacheablePolicy<T> {

    boolean support(Class<?> type);

    boolean isCacheable(T value);
}
