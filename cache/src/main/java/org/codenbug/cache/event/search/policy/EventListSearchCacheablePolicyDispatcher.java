package org.codenbug.cache.event.search.policy;

import java.lang.reflect.Field;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EventListSearchCacheablePolicyDispatcher {

    private final List<CacheablePolicy<?>> eventListFilterPolicies;

    public EventListSearchCacheablePolicyDispatcher() {
        this.eventListFilterPolicies = List.of(new EventListFilterCacheablePolicy(),
            new KeywordCacheablePolicy(), new PageOptionCacheablePolicy());
    }

    public boolean isCacheable(Field field, Object value) {
        if (value == null) {
            return true;
        }

        for (CacheablePolicy<?> policy : eventListFilterPolicies) {
            if (policy.support(field.getType())) {
                return invoke(policy, value);
            }
        }
        return false;
    }

    private <T> boolean invoke(CacheablePolicy<T> policy, Object value) {
        return policy.isCacheable((T) value);
    }
}
