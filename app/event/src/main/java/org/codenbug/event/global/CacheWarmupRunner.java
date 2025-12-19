package org.codenbug.event.global;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.application.cache.EventListSearchCache;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.codenbug.event.application.cache.EventListSearchCacheValue;
import org.codenbug.event.application.cache.PageOption;
import org.codenbug.event.application.cache.SortMethod;
import org.codenbug.event.application.cache.SortOption;
import org.codenbug.event.global.dto.CostRange;
import org.codenbug.event.global.dto.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.seat.domain.RegionLocation;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final List<CostRange> CACHEABLE_COST_RANGES = List.of(
        new CostRange(0, 10000),
        new CostRange(10001, 30000),
        new CostRange(30001, 50000)
    );

    private final EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> eventListSearchCache;
    private final EventViewRepository eventViewRepository;

    @Override
    public void run(ApplicationArguments args) {
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

        List<LocalDateTime> startDates = List.of(
            LocalDate.now().plusDays(1).atStartOfDay(),
            LocalDate.now().plusDays(7).atStartOfDay(),
            LocalDate.now().plusDays(30).atStartOfDay()
        );

        for (CostRange costRange : CACHEABLE_COST_RANGES) {
            filters.add(new EventListFilter.Builder().costRange(costRange).build());
        }
        for (LocalDateTime startDate : startDates) {
            filters.add(new EventListFilter.Builder().startDate(startDate).build());
        }

        RegionLocation regionLocation = RegionLocation.SEOUL;
        EventCategory category = new EventCategory(new CategoryId(1L), null, null);
        filters.add(new EventListFilter.Builder()
            .regionLocationList(List.of(regionLocation))
            .build());
        filters.add(new EventListFilter.Builder()
            .eventCategoryList(List.of(category))
            .build());

        for (LocalDateTime startDate : startDates) {
            for (CostRange costRange : CACHEABLE_COST_RANGES) {
                filters.add(new EventListFilter.Builder()
                    .startDate(startDate)
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
