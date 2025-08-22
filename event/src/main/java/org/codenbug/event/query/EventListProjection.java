package org.codenbug.event.query;

import java.time.LocalDateTime;

/**
 * Redis viewCount가 적용된 이벤트 리스트 조회용 Projection
 * DB viewCount 대신 Redis의 실시간 viewCount를 사용
 */
public class EventListProjection {
    private final String eventId;
    private final String title;
    private final String thumbnailUrl;
    private final LocalDateTime eventStart;
    private final LocalDateTime eventEnd;
    private final LocalDateTime bookingStart;
    private final LocalDateTime bookingEnd;
    private final Integer minPrice;
    private final Integer maxPrice;
    private final Integer dbViewCount; // DB의 원본 viewCount
    private Integer redisViewCount; // Redis의 실시간 viewCount
    private final String status;
    private final Long categoryId;
    private final String location;
    
    public EventListProjection(String eventId, String title, String thumbnailUrl,
                              LocalDateTime eventStart, LocalDateTime eventEnd,
                              LocalDateTime bookingStart, LocalDateTime bookingEnd,
                              Integer minPrice, Integer maxPrice, Integer dbViewCount,
                              String status, Long categoryId, String location) {
        this.eventId = eventId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.dbViewCount = dbViewCount;
        this.redisViewCount = dbViewCount; // 초기값은 DB 값
        this.status = status;
        this.categoryId = categoryId;
        this.location = location;
    }
    
    // Redis viewCount 설정
    public void setRedisViewCount(Integer redisViewCount) {
        this.redisViewCount = redisViewCount;
    }
    
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public LocalDateTime getEventStart() { return eventStart; }
    public LocalDateTime getEventEnd() { return eventEnd; }
    public LocalDateTime getBookingStart() { return bookingStart; }
    public LocalDateTime getBookingEnd() { return bookingEnd; }
    public Integer getMinPrice() { return minPrice; }
    public Integer getMaxPrice() { return maxPrice; }
    public Integer getDbViewCount() { return dbViewCount; }
    public Integer getViewCount() { return redisViewCount; } // 실제 사용되는 viewCount
    public String getStatus() { return status; }
    public Long getCategoryId() { return categoryId; }
    public String getLocation() { return location; }
    
}