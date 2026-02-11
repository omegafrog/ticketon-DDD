package org.codenbug.event.infra;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.SeatLayoutId;
import org.codenbug.message.SeatLayoutUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatLayoutUpdateEventListener {
    
    private final EventRepository eventRepository;

    @EventListener
    @Transactional
    public void handleSeatLayoutUpdated(SeatLayoutUpdatedEvent event) {
        try {
            log.info("[handleSeatLayoutUpdated] SeatLayout 업데이트 이벤트 수신. seatLayoutId: {}", event.getSeatLayoutId());
            
            // SeatLayoutId로 Event 찾기
            Event eventEntity = eventRepository.findBySeatLayoutId(new SeatLayoutId(event.getSeatLayoutId()));
            
            if (eventEntity != null) {
                // Event를 저장하여 @Version 필드 자동 증가
                eventRepository.save(eventEntity);
                
                log.info("[handleSeatLayoutUpdated] Event version 업데이트 완료. eventId: {}, seatLayoutId: {}, version: {}", 
                    eventEntity.getEventId().getEventId(), event.getSeatLayoutId(), eventEntity.getVersion());
            } else {
                log.warn("[handleSeatLayoutUpdated] SeatLayoutId에 해당하는 Event를 찾을 수 없습니다. seatLayoutId: {}", event.getSeatLayoutId());
            }
        } catch (Exception e) {
            log.error("[handleSeatLayoutUpdated] SeatLayout 업데이트 이벤트 처리 중 오류 발생. seatLayoutId: {}, 오류: {}", 
                event.getSeatLayoutId(), e.getMessage(), e);
            // 예외를 다시 던져서 트랜잭션 롤백
            throw new RuntimeException("SeatLayout 업데이트 이벤트 처리 실패", e);
        }
    }
}