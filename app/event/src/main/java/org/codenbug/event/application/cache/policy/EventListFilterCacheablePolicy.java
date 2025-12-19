package org.codenbug.event.application.cache.policy;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.event.global.dto.CostRange;
import org.codenbug.event.global.dto.EventListFilter;

@Slf4j
public class EventListFilterCacheablePolicy implements CacheablePolicy<EventListFilter> {

    public static final Supplier<Set<LocalDate>> START_DATE_FILTER_OPTIONS =
        () -> {
            LocalDate today = LocalDate.now();
            return Set.of(
                today.plusDays(30),
                today.plusDays(7),
                today.plusDays(1)
            );
        };

    public static final Supplier<Set<CostRange>> COST_RANGE_FILTER_OPTIONS =
        () -> {
            return Set.of(new CostRange(0, 10000),
                new CostRange(10001, 30000), new CostRange(30001, 50000));
        };

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
            cacheable &= START_DATE_FILTER_OPTIONS.get()
                .contains(value.getStartDate().toLocalDate());
        }

        if (value.getCostRange() != null) {
            cacheable &= COST_RANGE_FILTER_OPTIONS.get().contains(value.getCostRange());
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
