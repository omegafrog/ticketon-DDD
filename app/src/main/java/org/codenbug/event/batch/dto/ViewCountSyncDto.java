package org.codenbug.event.batch.dto;

/**
 * ViewCount 동기화를 위한 DTO
 * Redis -> DB 배치 처리에 사용
 */
public class ViewCountSyncDto {
    private String eventId;
    private Integer redisViewCount;
    private Integer dbViewCount;
    
    public ViewCountSyncDto() {}
    
    public ViewCountSyncDto(String eventId, Integer redisViewCount, Integer dbViewCount) {
        this.eventId = eventId;
        this.redisViewCount = redisViewCount;
        this.dbViewCount = dbViewCount;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public Integer getRedisViewCount() {
        return redisViewCount;
    }
    
    public void setRedisViewCount(Integer redisViewCount) {
        this.redisViewCount = redisViewCount;
    }
    
    public Integer getDbViewCount() {
        return dbViewCount;
    }
    
    public void setDbViewCount(Integer dbViewCount) {
        this.dbViewCount = dbViewCount;
    }
    
    /**
     * 동기화가 필요한지 판단
     */
    public boolean needsSync() {
        if (redisViewCount == null || dbViewCount == null) {
            return false;
        }
        return !redisViewCount.equals(dbViewCount);
    }
    
    /**
     * 증가된 조회수
     */
    public Integer getIncrementAmount() {
        if (redisViewCount == null || dbViewCount == null) {
            return 0;
        }
        return redisViewCount - dbViewCount;
    }
    
    @Override
    public String toString() {
        return "ViewCountSyncDto{" +
                "eventId='" + eventId + '\'' +
                ", redisViewCount=" + redisViewCount +
                ", dbViewCount=" + dbViewCount +
                ", increment=" + getIncrementAmount() +
                '}';
    }
}