package org.codenbug.event.application.cache;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.application.cache.policy.EventListFilterCacheablePolicy;
import org.codenbug.event.global.dto.CostRange;
import org.codenbug.event.global.dto.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.seat.domain.RegionLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CacheRefresher {

    private static final int DEFAULT_PAGE_SIZE = 30;

    private final EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> eventListSearchCache;
    private final EventViewRepository eventViewRepository;

    public CacheRefresher(
        EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> eventListSearchCache,
        EventViewRepository eventViewRepository) {
        this.eventListSearchCache = eventListSearchCache;
        this.eventViewRepository = eventViewRepository;
    }

    public void refreshAllCaches() {
        for (CacheWarmupTarget target : buildCacheableTargets()) {
            Pageable pageable = toPageable(target.pageOption());

            Page<EventListProjection> result = eventViewRepository.findEventList(
                target.keyword(), target.filter(), pageable);

            EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(
                target.filter(), target.keyword(), target.pageOption());

            if (!eventListSearchCache.exist(cacheKey)) {
                eventListSearchCache.put(cacheKey,
                    new EventListSearchCacheValue(result.getContent(),
                        (int) result.getTotalElements()));
            }
        }
    }

    private List<CacheWarmupTarget> buildCacheableTargets() {
        List<EventListFilter> filters = buildCacheableFilters();
        List<String> keywords = List.of("");
        List<PageOption> pageOptions = buildCacheablePageOptions();

        List<CacheWarmupTarget> targets = new ArrayList<>();
        for (EventListFilter filter : filters) {
            for (String keyword : keywords) {
                for (PageOption pageOption : pageOptions) {
                    EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(filter,
                        keyword, pageOption);
                    if (eventListSearchCache.isCacheable(cacheKey)) {
                        targets.add(new CacheWarmupTarget(filter, keyword, pageOption));
                    }
                }
            }
        }
        return targets;
    }

    private List<EventListFilter> buildCacheableFilters() {
        List<EventListFilter> filters = new ArrayList<>();
        filters.add(null);

        List<LocalDate> startDates = EventListFilterCacheablePolicy.START_DATE_FILTER_OPTIONS.get()
            .stream().toList();
        Set<CostRange> costRanges = EventListFilterCacheablePolicy.COST_RANGE_FILTER_OPTIONS.get();
        for (CostRange costRange : costRanges) {
            filters.add(new EventListFilter.Builder().costRange(costRange).build());
        }
        for (LocalDate startDate : startDates) {
            filters.add(new EventListFilter.Builder().startDate(startDate.atStartOfDay()).build());
        }

        RegionLocation regionLocation = RegionLocation.SEOUL;
        EventCategory category = new EventCategory(new CategoryId(1L), null, null);
        filters.add(new EventListFilter.Builder()
            .regionLocationList(List.of(regionLocation))
            .build());
        filters.add(new EventListFilter.Builder()
            .eventCategoryList(List.of(category))
            .build());

        for (LocalDate startDate : startDates) {
            for (CostRange costRange : costRanges) {
                filters.add(new EventListFilter.Builder()
                    .startDate(startDate.atStartOfDay())
                    .costRange(costRange)
                    .regionLocationList(List.of(regionLocation))
                    .eventCategoryList(List.of(category))
                    .build());
            }
        }

        return filters;
    }

    private List<PageOption> buildCacheablePageOptions() {
        List<SortOption> sortOptions = new ArrayList<>();
        for (SortMethod method : List.of(SortMethod.DATETIME, SortMethod.VIEW_COUNT,
            SortMethod.EVENT_START)) {
            sortOptions.add(new SortOption(method, true));
            sortOptions.add(new SortOption(method, false));
        }

        List<PageOption> pageOptions = new ArrayList<>();
        for (int page = 0; page < 5; page++) {
            for (SortOption sortOption : sortOptions) {
                pageOptions.add(new PageOption(page, List.of(sortOption)));
            }
        }
        return pageOptions;
    }

    private Pageable toPageable(PageOption pageOption) {
        Sort sort = Sort.unsorted();
        if (pageOption != null && pageOption.sortOptions() != null
            && !pageOption.sortOptions().isEmpty()) {
            List<Sort.Order> orders = pageOption.sortOptions().stream()
                .map(option -> new Sort.Order(
                    option.asc() ? Sort.Direction.ASC : Sort.Direction.DESC,
                    option.sortMethod().columnName
                ))
                .toList();
            sort = Sort.by(orders);
        }
        int page = pageOption != null ? pageOption.page() : 0;
        return PageRequest.of(page, DEFAULT_PAGE_SIZE, sort);
    }

    private record CacheWarmupTarget(EventListFilter filter, String keyword,
                                     PageOption pageOption) {

    }
}
