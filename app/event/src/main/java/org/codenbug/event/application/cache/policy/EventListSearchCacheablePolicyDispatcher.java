package org.codenbug.event.application.cache.policy;

import java.lang.reflect.Field;
import java.util.Set;
import org.codenbug.cachecore.event.search.CacheablePolicy;
import org.codenbug.cachecore.event.search.CacheablePolicyDispatcher;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.springframework.stereotype.Component;

@Component
public class EventListSearchCacheablePolicyDispatcher extends
    CacheablePolicyDispatcher<EventListSearchCacheKey> {

    public EventListSearchCacheablePolicyDispatcher() {
        super(Set.of(new EventListFilterCacheablePolicy(),
            new KeywordCacheablePolicy(), new PageOptionCacheablePolicy()));
    }

    @Override
    public boolean isCacheable(EventListSearchCacheKey cacheKey) {
        Class<? extends EventListSearchCacheKey> targetClass = cacheKey.getClass();
        Field[] fields = targetClass.getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                Object val = field.get(cacheKey);

                if (!fieldIsCacheable(field, val)) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return true;
    }

    public boolean fieldIsCacheable(Field field, Object value) {
        if (value == null) {
            return true;
        }

        for (CacheablePolicy<?> policy : policies) {
            if (policy.support(field.getType())) {
                return invoke(policy, value);
            }
        }
        return false;
    }

    private <T> boolean invoke(CacheablePolicy<T> policy,
        Object value) {
        return policy.isCacheable((T) value);
    }
}
