package org.codenbug.event.application.policy;

public class KeywordCacheablePolicy implements CacheablePolicy<String> {

    @Override
    public boolean support(Class<?> type) {
        return String.class.isAssignableFrom(type);
    }

    @Override
    public boolean isCacheable(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
