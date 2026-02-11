package org.codenbug.event.query;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Redis viewCount가 적용된 이벤트 리스트 조회용 Projection
 * DB viewCount 대신 Redis의 실시간 viewCount를 사용
 */
@Getter
@Setter
public class EventListProjection {
    private String eventId;
    private String title;
    private String thumbnailUrl;
    private LocalDateTime eventStart;
    private LocalDateTime eventEnd;
    private LocalDateTime bookingStart;
    private LocalDateTime bookingEnd;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer dbViewCount; // DB의 원본 viewCount
    private Integer redisViewCount; // Redis의 실시간 viewCount
    private String status;
    private Long categoryId;
    private String location;
    private Long seatCount;
    private Long availableSeatCount;
    
    // Kryo 직렬화를 위한 기본 생성자
    public EventListProjection() {}
    
    public EventListProjection(String eventId, String title, String thumbnailUrl,
                              LocalDateTime eventStart, LocalDateTime eventEnd,
                              LocalDateTime bookingStart, LocalDateTime bookingEnd,
                              Integer minPrice, Integer maxPrice, Integer dbViewCount,
                              String status, Long categoryId, String location, Long seatCount, Long availableSeatCount) {
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
		this.seatCount = seatCount;
		this.availableSeatCount = availableSeatCount;
	}
    public EventListProjection(String eventId, String title, String thumbnailUrl,
        LocalDateTime eventStart, LocalDateTime eventEnd,
        LocalDateTime bookingStart, LocalDateTime bookingEnd,
        Integer minPrice, Integer maxPrice, Integer dbViewCount,
        String status, Long categoryId, String location, Long seatCount) {
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
        this.seatCount = seatCount;
    }
    

    
}