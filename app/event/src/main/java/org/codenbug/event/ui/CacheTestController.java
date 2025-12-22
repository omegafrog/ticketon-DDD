package org.codenbug.event.ui;

import org.codenbug.message.EventCreatedEvent;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class CacheTestController {
    
    private final ApplicationEventPublisher eventPublisher;
    private final CacheKeyVersionManager versionManager;
    
    public CacheTestController(ApplicationEventPublisher eventPublisher,
        CacheKeyVersionManager versionManager) {
        this.eventPublisher = eventPublisher;
        this.versionManager = versionManager;
    }
    
    @PostMapping("/cache-invalidation")
    public ResponseEntity<String> testCacheInvalidation() {
        // 테스트용 EventCreatedEvent 발행
        EventCreatedEvent testEvent = new EventCreatedEvent(
            "test-event-id",
            "테스트 이벤트", 
            "test-manager-id",
            1L,
            true,
            "테스트 장소",
            "2025-01-01T10:00:00",
            "2025-01-01T12:00:00",
            10000,
            50000,
            1L
        );
        
        eventPublisher.publishEvent(testEvent);
        
        return ResponseEntity.ok("Cache invalidation event published successfully");
    }

    @PostMapping("/cache-epoch/bump")
    public ResponseEntity<String> bumpCacheEpoch() {
        long version = versionManager.bumpVersion(null);
        return ResponseEntity.ok("Cache epoch bumped to " + version);
    }
}
