package org.codenbug.event.query;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Set;

/**
 * Redis를 이용한 Event viewCount 캐시 관리 서비스
 * 성능 향상을 위해 Redis에서 viewCount를 관리하고 배치로 DB 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisViewCountService {
    
    @Qualifier("redisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis Key 패턴
    private static final String VIEW_COUNT_KEY_PREFIX = "event:viewCount:";
    private static final String VIEW_COUNT_BATCH_LOCK = "event:viewCount:batch:lock";
    
    /**
     * Event viewCount 조회 (Redis 우선, 없으면 DB에서 조회 후 캐싱)
     */
    public Integer getViewCount(String eventId, Integer dbViewCount) {
        String key = generateViewCountKey(eventId);
        
        try {
            Object cachedCount = redisTemplate.opsForValue().get(key);
            
            if (cachedCount != null) {
                return Integer.parseInt(cachedCount.toString());
            } else {
                // Redis에 없으면 DB 값으로 초기화
                if (dbViewCount != null && dbViewCount > 0) {
                    setViewCount(eventId, dbViewCount);
                    return dbViewCount;
                } else {
                    // DB에도 없으면 0으로 초기화
                    setViewCount(eventId, 0);
                    return 0;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get viewCount from Redis for event: {}, error: {}", eventId, e.getMessage());
            // Redis 실패 시 DB 값 반환
            return dbViewCount != null ? dbViewCount : 0;
        }
    }
    
    /**
     * Event viewCount 조회 (리스트용 - Redis에 없으면 DB 값만 반환, 캐싱하지 않음)
     */
    public Integer getViewCountForList(String eventId, Integer dbViewCount) {
        String key = generateViewCountKey(eventId);
        
        try {
            Object cachedCount = redisTemplate.opsForValue().get(key);
            
            if (cachedCount != null) {
                return Integer.parseInt(cachedCount.toString());
            } else {
                // Redis에 없으면 DB 값 그대로 반환 (캐싱하지 않음)
                return dbViewCount != null ? dbViewCount : 0;
            }
        } catch (Exception e) {
            log.warn("Failed to get viewCount from Redis for event: {}, error: {}", eventId, e.getMessage());
            // Redis 실패 시 DB 값 반환
            return dbViewCount != null ? dbViewCount : 0;
        }
    }
    
    /**
     * Event viewCount 증가
     */
    public Long incrementViewCount(String eventId) {
        String key = generateViewCountKey(eventId);
        
        try {
            Long newCount = redisTemplate.opsForValue().increment(key);
            
            // TTL 설정 (24시간) - 배치 주기보다 길게 설정
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            
            log.debug("Incremented viewCount for event: {} to {}", eventId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Failed to increment viewCount in Redis for event: {}, error: {}", eventId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Event viewCount 설정
     */
    public void setViewCount(String eventId, Integer count) {
        String key = generateViewCountKey(eventId);
        
        try {
            redisTemplate.opsForValue().set(key, count.toString(), 24, TimeUnit.HOURS);
            log.debug("Set viewCount for event: {} to {}", eventId, count);
        } catch (Exception e) {
            log.error("Failed to set viewCount in Redis for event: {}, error: {}", eventId, e.getMessage());
        }
    }
    
    /**
     * 모든 viewCount 데이터 조회 (배치 처리용)
     */
    public Map<Object, Object> getAllViewCounts() {
        try {
            String pattern = VIEW_COUNT_KEY_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Map.of();
            }
            
            return redisTemplate.opsForHash().entries("event:viewCount:all");
        } catch (Exception e) {
            log.error("Failed to get all viewCounts from Redis, error: {}", e.getMessage());
            return Map.of();
        }
    }
    
    /**
     * 배치 처리를 위한 viewCount 스냅샷 생성
     */
    public Map<String, Integer> createViewCountSnapshot() {
        try {
            String pattern = VIEW_COUNT_KEY_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Map.of();
            }
            
            Map<String, Integer> snapshot = new java.util.HashMap<>();
            
            for (String key : keys) {
                String eventId = extractEventIdFromKey(key);
                Object value = redisTemplate.opsForValue().get(key);
                
                if (value != null) {
                    snapshot.put(eventId, Integer.parseInt(value.toString()));
                }
            }
            
            log.info("Created viewCount snapshot with {} events", snapshot.size());
            return snapshot;
        } catch (Exception e) {
            log.error("Failed to create viewCount snapshot, error: {}", e.getMessage());
            return Map.of();
        }
    }
    
    /**
     * 배치 처리 후 Redis 데이터 정리 (선택적)
     */
    public void clearProcessedViewCounts(Set<String> eventIds) {
        try {
            for (String eventId : eventIds) {
                String key = generateViewCountKey(eventId);
                // 완전 삭제 대신 TTL 갱신으로 유지
                redisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
            log.info("Updated TTL for {} viewCount entries", eventIds.size());
        } catch (Exception e) {
            log.error("Failed to clear processed viewCounts, error: {}", e.getMessage());
        }
    }
    
    /**
     * 배치 처리 락 획득
     */
    public boolean acquireBatchLock() {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(VIEW_COUNT_BATCH_LOCK, "locked", 13, TimeUnit.HOURS); // 12시간 + 1시간 여유
            return acquired != null && acquired;
        } catch (Exception e) {
            log.error("Failed to acquire batch lock, error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 배치 처리 락 해제
     */
    public void releaseBatchLock() {
        try {
            redisTemplate.delete(VIEW_COUNT_BATCH_LOCK);
            log.info("Released batch lock");
        } catch (Exception e) {
            log.error("Failed to release batch lock, error: {}", e.getMessage());
        }
    }
    
    private String generateViewCountKey(String eventId) {
        return VIEW_COUNT_KEY_PREFIX + eventId;
    }
    
    private String extractEventIdFromKey(String key) {
        return key.substring(VIEW_COUNT_KEY_PREFIX.length());
    }
}