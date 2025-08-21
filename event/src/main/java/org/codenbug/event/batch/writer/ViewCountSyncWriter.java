package org.codenbug.event.batch.writer;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.codenbug.event.batch.dto.ViewCountSyncDto;
import org.codenbug.event.domain.QEvent;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ViewCount 동기화를 위한 ItemWriter
 * 청크 단위로 bulk update를 수행하여 성능 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncWriter implements ItemWriter<ViewCountSyncDto> {
    
    private final JPAQueryFactory queryFactory;
    private final QEvent event = QEvent.event;
    
    @Override
    @Transactional
    public void write(Chunk<? extends ViewCountSyncDto> chunk) {
        List<? extends ViewCountSyncDto> items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }
        
        log.info("Writing {} viewCount sync items", items.size());
        
        int totalUpdated = 0;
        
        // 각 아이템에 대해 개별 업데이트 수행
        for (ViewCountSyncDto syncDto : items) {
            try {
                long updated = queryFactory
                    .update(event)
                    .set(event.eventInformation.viewCount, syncDto.getRedisViewCount())
                    .where(event.eventId.eventId.eq(syncDto.getEventId()))
                    .execute();
                
                if (updated > 0) {
                    totalUpdated++;
                    log.debug("Updated viewCount for event: {} from {} to {}", 
                             syncDto.getEventId(), 
                             syncDto.getDbViewCount(), 
                             syncDto.getRedisViewCount());
                } else {
                    log.warn("No event found for update: eventId={}", syncDto.getEventId());
                }
                
            } catch (Exception e) {
                log.error("Failed to update viewCount for event: {}, error: {}", 
                         syncDto.getEventId(), e.getMessage());
                // 개별 아이템 실패가 전체 청크 실패로 이어지지 않도록 예외를 먹음
                // 필요하다면 실패한 아이템을 별도로 기록할 수 있음
            }
        }
        
        log.info("Successfully updated {} out of {} viewCount items", totalUpdated, items.size());
        
        // 업데이트 통계 로깅
        if (log.isInfoEnabled()) {
            int totalIncrement = items.stream()
                .mapToInt(ViewCountSyncDto::getIncrementAmount)
                .sum();
            log.info("Total viewCount increment in this chunk: {}", totalIncrement);
        }
    }
}