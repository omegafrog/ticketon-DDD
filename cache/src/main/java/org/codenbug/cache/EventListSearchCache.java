package org.codenbug.cache;

import com.github.benmanes.caffeine.cache.Cache;
import java.lang.reflect.Field;
import org.codenbug.event.application.EventListSearchCacheKey;
import org.codenbug.event.application.EventListSearchCacheValue;
import org.codenbug.event.application.policy.EventListSearchCacheablePolicyDispatcher;
import org.springframework.stereotype.Component;

@Component
public class EventListSearchCache implements
    org.codenbug.event.application.EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> {

    private final Cache cache;
    private final EventListSearchCacheablePolicyDispatcher cacheablePolicyDispatcher;

    public EventListSearchCache(Cache cache,
        EventListSearchCacheablePolicyDispatcher cacheablePolicyDispatcher) {
        this.cache = cache;
        this.cacheablePolicyDispatcher = cacheablePolicyDispatcher;
    }

    @Override
    public void put(EventListSearchCacheKey cacheKey, EventListSearchCacheValue eventPage) {
        cache.put(cacheKey, eventPage);
    }

    @Override
    public EventListSearchCacheValue get(EventListSearchCacheKey key) {
        EventListSearchCacheValue result = (EventListSearchCacheValue) cache.getIfPresent(key);
        return result;
    }


    @Override
    public boolean isCacheable(EventListSearchCacheKey cacheKey) {
        Class<? extends EventListSearchCacheKey> targetClass = cacheKey.getClass();
        Field[] fields = targetClass.getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                Object val = field.get(cacheKey);

                if (!cacheablePolicyDispatcher.isCacheable(field, val)) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return true;
    }


    @Override
    public boolean exist(EventListSearchCacheKey cacheKey) {
        return cache.getIfPresent(cacheKey) != null;
    }
}
