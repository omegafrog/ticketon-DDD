package org.codenbug.event.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_layout_stats")
public class SeatLayoutStats {
    
    @Id
    @Column(name = "layout_id")
    private Long layoutId;
    
    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // 기본 생성자 (JPA 필수)
    protected SeatLayoutStats() {}
    
    public SeatLayoutStats(Long layoutId, Integer seatCount) {
        this.layoutId = layoutId;
        this.seatCount = seatCount;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters
    public Long getLayoutId() {
        return layoutId;
    }
    
    public Integer getSeatCount() {
        return seatCount;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}