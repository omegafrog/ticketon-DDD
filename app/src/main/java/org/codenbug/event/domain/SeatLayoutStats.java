package org.codenbug.event.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seat_layout_stats")
public class SeatLayoutStats {
    
    @Id
    @Column(name = "layout_id")
    private Long layoutId;
    
    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;

    @Column(name = "min_price", nullable = false)
    private Integer minPrice;

    @Column(name = "max_price", nullable = false)
    private Integer maxPrice;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // 기본 생성자 (JPA 필수)
    protected SeatLayoutStats() {}
    
    public SeatLayoutStats(Long layoutId, Integer seatCount) {
        this.layoutId = layoutId;
        this.seatCount = seatCount;
        this.minPrice = 0;
        this.maxPrice = 0;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters
    public Long getLayoutId() {
        return layoutId;
    }
    
    public Integer getSeatCount() {
        return seatCount;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}
