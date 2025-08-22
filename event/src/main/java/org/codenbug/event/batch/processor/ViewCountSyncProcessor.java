package org.codenbug.event.batch.processor;

import org.codenbug.event.batch.dto.ViewCountSyncDto;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * ViewCount 동기화를 위한 ItemProcessor
 * 비즈니스 로직이나 데이터 변환이 필요한 경우 처리
 * 현재는 단순 pass-through이지만 향후 확장 가능
 */
@Slf4j
@Component
public class ViewCountSyncProcessor implements ItemProcessor<ViewCountSyncDto, ViewCountSyncDto> {
    
    @Override
    public ViewCountSyncDto process(ViewCountSyncDto item) {
        // 검증 로직
        if (item == null || item.getEventId() == null || item.getRedisViewCount() == null) {
            log.warn("Invalid sync data: {}", item);
            return null; // null 반환 시 해당 아이템은 writer로 전달되지 않음
        }
        
        // 비정상적인 조회수 증가 체크 (예: 하루에 100만 이상 증가)
        int increment = item.getIncrementAmount();
        // if (increment > 1_000_000) {
        //     log.warn("Suspicious view count increment detected: eventId={}, increment={}",
        //              item.getEventId(), increment);
        //     // 의심스러운 증가는 일단 경고만 남기고 처리는 진행
        // }
        
        // 음수 조회수는 처리하지 않음
        if (item.getRedisViewCount() < 0) {
            log.warn("Negative view count detected: eventId={}, count={}", 
                     item.getEventId(), item.getRedisViewCount());
            return null;
        }
        
        log.debug("Processing sync data: eventId={}, increment={}", 
                  item.getEventId(), increment);
        
        return item;
    }
}