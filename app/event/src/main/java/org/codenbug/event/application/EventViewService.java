package org.codenbug.event.application;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.cachecore.event.search.CacheClient;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.codenbug.event.application.cache.EventListSearchCacheValue;
import org.codenbug.event.application.cache.policy.EventListSearchCacheablePolicyDispatcher;
import org.codenbug.event.application.dto.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.seat.domain.RegionLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventViewService {

    private final EventViewRepository eventViewRepository;
    private final CacheClient<EventListSearchCacheKey, EventListSearchCacheValue> searchCache;
    private final EventListSearchCacheablePolicyDispatcher dispatcher;
    private final CacheKeyVersionManager versionManager;

    public EventViewService(EventViewRepository eventViewRepository,
        CacheClient searchCache, EventListSearchCacheablePolicyDispatcher dispatcher,
        CacheKeyVersionManager versionManager) {
        this.eventViewRepository = eventViewRepository;
        this.searchCache = searchCache;
        this.dispatcher = dispatcher;
        this.versionManager = versionManager;
    }

    public Page<EventListProjection> getEventSearchResult(String keyword,
        @Valid EventListFilter filter, Pageable pageable) {
        RegionLocation regionLocation = filter != null ? filter.getSingleRegionLocation() : null;
        long epoch = versionManager.getVersion(resolveRegionKey(regionLocation));
        EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(epoch, filter, keyword,
            pageable);
        if (searchCache.exist(cacheKey)) {
            log.debug("Cache hit: {}", cacheKey);
            EventListSearchCacheValue result = searchCache.get(cacheKey, () -> {
                Page<EventListProjection> loaded = eventViewRepository.findEventList(keyword,
                    filter, pageable);
                return new EventListSearchCacheValue(loaded.getContent(),
                    (int) loaded.getTotalElements());

            });
            return new PageImpl<>(result.eventListProjection(), pageable, result.total());
        }

        Page<EventListProjection> result = eventViewRepository.findEventList(keyword, filter,
            pageable);
        if (dispatcher.isCacheable(cacheKey)) {
            log.debug("Cache miss: {}", cacheKey);
            searchCache.put(cacheKey, new EventListSearchCacheValue(result.getContent(),
                (int) result.getTotalElements()));
        }

        return result;
    }

    private String resolveRegionKey(RegionLocation regionLocation) {
        return regionLocation != null ? regionLocation.name() : null;
    }
}
