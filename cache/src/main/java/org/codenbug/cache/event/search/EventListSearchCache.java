package org.codenbug.cache.event.search;

import ch.qos.logback.core.util.TimeUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.codenbug.event.application.*;
import org.codenbug.event.global.CostRange;
import org.codenbug.event.global.EventListFilter;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Set;

@Component
public class EventListSearchCache implements org.codenbug.event.application.EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> {
    private final Cache cache;
    private static final LocalDate today = LocalDate.now();
    private static final Set<LocalDate> startDateFilterOptions = Set.of(today.minusDays(30), today.minusDays(7), today.minusDays(1));
    private static final Set<CostRange> costRangeFilterOptions = Set.of(new CostRange(0, 10000), new CostRange(10001, 30000), new CostRange(30001, 50000));

    public EventListSearchCache(Cache cache) {
        this.cache = cache;
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
        EventListFilter filter = cacheKey.filter();
        String keyword = cacheKey.keyword();
        PageOption pageOption = cacheKey.pageOption();

        return isCacheable(filter) && isCacheable(keyword) && isCacheable(pageOption);
    }

    private static boolean isCacheable(EventListFilter filter){
        return startDateFilterOptions.contains(filter.getStartDate()) &&
                (filter.getRegionLocationList().size() == 1) &&
                costRangeFilterOptions.contains(filter.getCostRange()) &&
                (filter.getEventCategoryList().size() == 1);
    }
    private static boolean isCacheable(String keyword){
        return true;
    }
    private static boolean isCacheable(PageOption pageOption){
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
