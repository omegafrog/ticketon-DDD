package org.codenbug.cache.event.search.policy;

import java.time.LocalDate;
import java.util.Set;
import org.codenbug.event.global.CostRange;
import org.codenbug.event.global.EventListFilter;

public class EventListFilterCacheablePolicy implements CacheablePolicy<EventListFilter> {

    private static final LocalDate today = LocalDate.now();
    private static final Set<LocalDate> startDateFilterOptions = Set.of(today.plusDays(30),
        today.plusDays(7), today.plusDays(1));
    private static final Set<CostRange> costRangeFilterOptions = Set.of(new CostRange(0, 10000),
        new CostRange(10001, 30000), new CostRange(30001, 50000));

    @Override
    public boolean support(Class<?> type) {
        return EventListFilter.class.isAssignableFrom(type);
    }

    @Override
    public boolean isCacheable(EventListFilter value) {
        if (value == null) {
            return true;
        }

        LocalDate startDate = value.getStartDate() == null ? null : value.getStartDate()
            .toLocalDate();

        return startDateFilterOptions.contains(startDate) &&
            (value.getRegionLocationList().size() == 1) &&
            costRangeFilterOptions.contains(value.getCostRange()) &&
            (value.getEventCategoryList().size() == 1);
    }
}
