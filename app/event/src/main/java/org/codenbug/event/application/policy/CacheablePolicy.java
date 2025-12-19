package org.codenbug.event.application.policy;

public interface CacheablePolicy<T> {

    boolean support(Class<?> type);

    boolean isCacheable(T value);
}
