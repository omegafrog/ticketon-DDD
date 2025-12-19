package org.codenbug.event.application.cache.policy;

import org.codenbug.event.application.cache.PageOption;
import org.codenbug.event.application.cache.SortMethod;

public class PageOptionCacheablePolicy implements CacheablePolicy<PageOption> {

    @Override
    public boolean support(Class<?> type) {
        return PageOption.class.isAssignableFrom(type);
    }

    @Override
    public boolean isCacheable(PageOption value) {
        if (value == null) {
            return true;
        }

        return value.page() < 5 &&
            value.sortOptions().stream().allMatch(option ->
                option.sortMethod().equals(SortMethod.DATETIME) ||
                    option.sortMethod().equals(SortMethod.VIEW_COUNT) ||
                    option.sortMethod().equals(SortMethod.EVENT_START));
    }
}
