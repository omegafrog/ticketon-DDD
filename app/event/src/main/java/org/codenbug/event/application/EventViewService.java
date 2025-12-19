package org.codenbug.event.application;

import jakarta.validation.Valid;
import org.codenbug.event.application.cache.EventListSearchCache;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.codenbug.event.application.cache.EventListSearchCacheValue;
import org.codenbug.event.global.dto.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventViewService {

    private final EventViewRepository eventViewRepository;
    private final EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> searchCache;

    public EventViewService(EventViewRepository eventViewRepository,
        EventListSearchCache searchCache) {
        this.eventViewRepository = eventViewRepository;
        this.searchCache = searchCache;
    }

    public Page<EventListProjection> getEventSearchResult(String keyword,
        @Valid EventListFilter filter, Pageable pageable) {
        EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(filter, keyword, pageable);
        if (searchCache.exist(cacheKey)) {
            EventListSearchCacheValue result = searchCache.get(cacheKey);
            return new PageImpl<>(result.eventListProjection(), pageable, result.total());
        }

        Page<EventListProjection> result = eventViewRepository.findEventList(keyword, filter,
            pageable);
        if (searchCache.isCacheable(cacheKey)) {
            searchCache.put(cacheKey, new EventListSearchCacheValue(result.getContent(),
                (int) result.getTotalElements()));
        }

        return result;
    }
}
