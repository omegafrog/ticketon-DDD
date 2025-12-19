package org.codenbug.cache.event.search.policy;

import java.time.LocalDate;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.event.global.CostRange;
import org.codenbug.event.global.EventListFilter;

@Slf4j
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
        boolean cacheable = true;
        if (value.getStartDate() != null) {
            cacheable &= startDateFilterOptions.contains(value.getStartDate().toLocalDate());
        }

        if (value.getCostRange() != null) {
            cacheable &= costRangeFilterOptions.contains(value.getCostRange());
        }
        if (value.getRegionLocationList() != null) {
            cacheable &= value.getRegionLocationList().size() == 1;
        }
        if (value.getEventCategoryList() != null) {
            cacheable &= value.getEventCategoryList().size() == 1;
        }

        if (cacheable) {
            log.info("캐싱됨");
        }
        return cacheable;
    }
}
