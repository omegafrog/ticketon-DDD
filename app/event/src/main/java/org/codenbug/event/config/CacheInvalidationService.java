package org.codenbug.event.config;

import org.codenbug.message.EventCreatedEvent;
import org.codenbug.message.EventDeletedEvent;
import org.codenbug.message.EventUpdatedEvent;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationService {
    
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    
    @EventListener
    public void handleEventCreated(EventCreatedEvent event) {
        log.info("Handling event created: {}", event.getEventId());
        
        Cache eventSearchCache = cacheManager.getCache("eventSearch");
        if (eventSearchCache != null) {
            eventSearchCache.evict("single:" + event.getEventId());
        }
        
        invalidateMatchingEventListCaches(
            event.getTitle(), 
            event.getMinPrice(), 
            event.getMaxPrice(), 
            event.getCategoryId(),
            event.getLocation()
        );
    }
    
    @EventListener
    public void handleEventUpdated(EventUpdatedEvent event) {
        log.info("Handling event updated: {}", event.getEventId());
        
        Cache eventSearchCache = cacheManager.getCache("eventSearch");
        if (eventSearchCache != null) {
            eventSearchCache.evict("single:" + event.getEventId());
        }
        
        invalidateMatchingEventListCaches(
            event.getTitle(), 
            event.getMinPrice(), 
            event.getMaxPrice(), 
            event.getCategoryId(),
            event.getLocation()
        );
    }
    
    @EventListener
    public void handleEventDeleted(EventDeletedEvent event) {
        log.info("Handling event deleted: {}", event.getEventId());
        
        Cache eventSearchCache = cacheManager.getCache("eventSearch");
        if (eventSearchCache != null) {
            eventSearchCache.evict("single:" + event.getEventId());
        }
        
        invalidateMatchingEventListCaches(
            event.getTitle(), 
            event.getMinPrice(), 
            event.getMaxPrice(), 
            event.getCategoryId(),
            event.getLocation()
        );
    }
    
    private void invalidateMatchingEventListCaches(String title, Integer minPrice, Integer maxPrice, Long categoryId, String location) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> cacheKeys = keys.getKeysByPattern("eventSearch:*");
        
        for (String cacheKey : cacheKeys) {
            try {
                if (shouldInvalidateCache(cacheKey, title, minPrice, maxPrice, categoryId, location)) {
                    redissonClient.getBucket(cacheKey).delete();
                    log.debug("Invalidated cache key: {}", cacheKey);
                }
            } catch (Exception e) {
                log.warn("Failed to process cache key: {}", cacheKey, e);
            }
        }
    }
    
    private boolean shouldInvalidateCache(String cacheKey, String eventTitle, Integer eventMinPrice, Integer eventMaxPrice, Long eventCategoryId, String eventLocation) {
        try {
            String keyContent = redissonClient.getBucket(cacheKey).get().toString();
            Map<String, Object> cacheParams = extractCacheParams(keyContent);
            
            if (cacheParams == null) {
                return true;
            }
            
            return matchesKeywordFilter(cacheParams, eventTitle) ||
                   matchesPriceFilter(cacheParams, eventMinPrice, eventMaxPrice) ||
                   matchesCategoryFilter(cacheParams, eventCategoryId) ||
                   matchesLocationFilter(cacheParams, eventLocation) ||
                   hasNoFilters(cacheParams);
                   
        } catch (Exception e) {
            log.warn("Error checking cache invalidation for key: {}", cacheKey, e);
            return true;
        }
    }
    
    private Map<String, Object> extractCacheParams(String keyContent) {
        try {
            if (keyContent.contains("keyword=") || keyContent.contains("filter=")) {
                return parseKeyParams(keyContent);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract cache params from: {}", keyContent, e);
            return null;
        }
    }
    
    private Map<String, Object> parseKeyParams(String keyContent) throws JsonProcessingException {
        if (keyContent.contains("{") && keyContent.contains("}")) {
            String jsonPart = keyContent.substring(keyContent.indexOf("{"), keyContent.lastIndexOf("}") + 1);
            return objectMapper.readValue(jsonPart, new TypeReference<Map<String, Object>>() {});
        }
        return Map.of();
    }
    
    private boolean matchesKeywordFilter(Map<String, Object> cacheParams, String eventTitle) {
        String keyword = (String) cacheParams.get("keyword");
        if (keyword != null && eventTitle != null) {
            return eventTitle.toLowerCase().contains(keyword.toLowerCase());
        }
        return false;
    }
    
    private boolean matchesPriceFilter(Map<String, Object> cacheParams, Integer eventMinPrice, Integer eventMaxPrice) {
        Map<String, Object> costRange = (Map<String, Object>) cacheParams.get("costRange");
        if (costRange != null && eventMinPrice != null && eventMaxPrice != null) {
            Integer filterMin = (Integer) costRange.get("min");
            Integer filterMax = (Integer) costRange.get("max");
            
            if (filterMin != null && filterMax != null) {
                return !(eventMaxPrice < filterMin || eventMinPrice > filterMax);
            }
        }
        return false;
    }
    
    private boolean matchesCategoryFilter(Map<String, Object> cacheParams, Long eventCategoryId) {
        List<Long> categoryList = (List<Long>) cacheParams.get("eventCategoryList");
        if (categoryList != null && eventCategoryId != null) {
            return categoryList.contains(eventCategoryId);
        }
        return false;
    }
    
    private boolean matchesLocationFilter(Map<String, Object> cacheParams, String eventLocation) {
        List<String> locationList = (List<String>) cacheParams.get("locationList");
        if (locationList != null && eventLocation != null) {
            return locationList.contains(eventLocation);
        }
        return false;
    }
    
    private boolean hasNoFilters(Map<String, Object> cacheParams) {
        return (cacheParams.get("keyword") == null || ((String) cacheParams.get("keyword")).trim().isEmpty()) &&
               cacheParams.get("costRange") == null &&
               (cacheParams.get("eventCategoryList") == null || ((List<?>) cacheParams.get("eventCategoryList")).isEmpty()) &&
               (cacheParams.get("locationList") == null || ((List<?>) cacheParams.get("locationList")).isEmpty());
    }
}