package org.codenbug.event.application.cache;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.cachecore.event.search.CacheClient;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.application.cache.policy.EventListFilterCacheablePolicy;
import org.codenbug.event.application.cache.policy.EventListSearchCacheablePolicyDispatcher;
import org.codenbug.event.application.dto.CostRange;
import org.codenbug.event.application.dto.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.seat.domain.RegionLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheRefresher {

    private static final int DEFAULT_PAGE_SIZE = 30;

    private final CacheClient<EventListSearchCacheKey, EventListSearchCacheValue> eventListSearchCache;
    private final CacheKeyVersionManager versionManager;
    private final EventListSearchCacheablePolicyDispatcher dispatcher;
    private final EventViewRepository eventViewRepository;

    public CacheRefresher(
        CacheClient<EventListSearchCacheKey, EventListSearchCacheValue> eventListSearchCache,
        CacheKeyVersionManager versionManager,
        EventListSearchCacheablePolicyDispatcher dispatcher,
        EventViewRepository eventViewRepository) {
        this.eventListSearchCache = eventListSearchCache;
        this.versionManager = versionManager;
        this.dispatcher = dispatcher;
        this.eventViewRepository = eventViewRepository;
    }

    public void refreshAllCaches() {
        for (CacheWarmupTarget target : buildCacheableTargets()) {
            log.debug("Warming up cache: {}", target);
            Pageable pageable = toPageable(target.pageOption());

            Page<EventListProjection> result = eventViewRepository.findEventList(
                target.keyword(), target.filter(), pageable);

            RegionLocation regionLocation =
                target.filter() != null ? target.filter().getSingleRegionLocation() : null;
            EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(
                versionManager.getVersion(resolveRegionKey(regionLocation)), target.filter(),
                target.keyword(),
                target.pageOption());

            if (!eventListSearchCache.exist(cacheKey)) {
                eventListSearchCache.put(cacheKey,
                    new EventListSearchCacheValue(result.getContent(),
                        (int) result.getTotalElements()));
            }
        }
    }

    private List<CacheWarmupTarget> buildCacheableTargets() {
        List<EventListFilter> filters = buildCacheableFilters();
        List<PageOption> pageOptions = buildCacheablePageOptions();

        List<CacheWarmupTarget> targets = new ArrayList<>();
        for (EventListFilter filter : filters) {
            for (PageOption pageOption : pageOptions) {
                RegionLocation regionLocation =
                    filter != null ? filter.getSingleRegionLocation() : null;
                EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(
                    versionManager.getVersion(resolveRegionKey(regionLocation)), filter,
                    null, pageOption);
                if (dispatcher.isCacheable(cacheKey)) {
                    targets.add(new CacheWarmupTarget(filter, null, pageOption));
                }
            }
        }
        return targets;
    }

    private List<EventListFilter> buildCacheableFilters() {
        List<EventListFilter> filters = new ArrayList<>();

        List<LocalDate> startDates = EventListFilterCacheablePolicy.START_DATE_FILTER_OPTIONS.get()
            .stream().toList();
        Set<CostRange> costRanges = EventListFilterCacheablePolicy.COST_RANGE_FILTER_OPTIONS.get();
        List<RegionLocation> regionOptions = buildRegionOptions();

        addFiltersWithRegion(filters, regionOptions, null, null, null);
        for (CostRange costRange : costRanges) {
            addFiltersWithRegion(filters, regionOptions, costRange, null, null);
        }
        for (LocalDate startDate : startDates) {
            addFiltersWithRegion(filters, regionOptions, null, startDate, null);
        }

        for (LocalDate startDate : startDates) {
            for (CostRange costRange : costRanges) {
                addFiltersWithRegion(filters, regionOptions, costRange, startDate,
                    List.of());
            }
        }

        return filters;
    }

    private List<PageOption> buildCacheablePageOptions() {
        List<SortOption> sortOptions = new ArrayList<>();
        sortOptions.add(null);
        for (SortMethod method : List.of(SortMethod.DATETIME, SortMethod.VIEW_COUNT,
            SortMethod.EVENT_START)) {
            sortOptions.add(new SortOption(method, true));
            sortOptions.add(new SortOption(method, false));
        }

        List<PageOption> pageOptions = new ArrayList<>();
        for (int page = 0; page < 5; page++) {
            for (SortOption sortOption : sortOptions) {
                if (sortOption == null) {
                    pageOptions.add(new PageOption(page, null));
                } else {
                    pageOptions.add(new PageOption(page, List.of(sortOption)));
                }
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

    private List<RegionLocation> buildRegionOptions() {
        List<RegionLocation> options = new ArrayList<>();
        options.add(null);
        options.addAll(List.of(RegionLocation.values()));
        return options;
    }

    private void addFiltersWithRegion(List<EventListFilter> filters,
        List<RegionLocation> regionOptions, CostRange costRange, LocalDate startDate,
        List<EventCategory> eventCategories) {
        for (RegionLocation regionLocation : regionOptions) {
            EventListFilter.Builder builder = new EventListFilter.Builder()
                .costRange(costRange)
                .startDate(startDate == null ? null : startDate.atStartOfDay())
                .eventCategoryList(eventCategories);

            if (regionLocation != null) {
                builder.regionLocationList(List.of(regionLocation));
            }

            EventListFilter filter = builder.build();
            if (filter.canFiltered()) {
                filters.add(filter);
            } else if (regionLocation == null) {
                filters.add(null);
            }
        }
    }

    private String resolveRegionKey(RegionLocation regionLocation) {
        return regionLocation != null ? regionLocation.name() : null;
    }

    private record CacheWarmupTarget(EventListFilter filter, String keyword,
                                     PageOption pageOption) {

        @Override
        public String toString() {
            return "CacheWarmupTarget{" +
                "filter=" + filter +
                ", keyword='" + keyword + '\'' +
                ", pageOption=" + pageOption +
                '}';
        }
    }
}
