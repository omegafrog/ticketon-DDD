package org.codenbug.cachecore.event.search;

import java.util.Set;

public abstract class CacheablePolicyDispatcher<K> {

    protected final Set<CacheablePolicy<?>> policies;

    protected CacheablePolicyDispatcher(Set<CacheablePolicy<?>> policies) {
        this.policies = policies;
    }

    public abstract boolean isCacheable(K key);
}
