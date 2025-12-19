package org.codenbug.cache.event.search;

import java.lang.reflect.Field;
import org.codenbug.cache.event.search.policy.EventListSearchCacheablePolicyDispatcher;
import org.codenbug.event.application.EventListSearchCacheKey;
import org.codenbug.event.application.EventListSearchCacheValue;
import org.codenbug.event.application.PageOption;
import org.codenbug.event.application.SortMethod;
import org.springframework.cache.Cache;
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
        EventListSearchCacheValue result = (EventListSearchCacheValue) cache.get(key).get();
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

                if (val == null) {
                    return true;
                }
                if (!cacheablePolicyDispatcher.isCacheable(field, val)) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return true;
    }

    private static boolean isCacheable(PageOption pageOption) {
        return pageOption.page() < 5 &&
            pageOption.sortOptions().stream().allMatch(option ->
                option.sortMethod().equals(SortMethod.DATETIME) ||
                    option.sortMethod().equals(SortMethod.VIEW_COUNT) ||
                    option.sortMethod().equals(SortMethod.EVENT_START));
    }


    @Override
    public boolean exist(EventListSearchCacheKey cacheKey) {
        return false;
    }
}
