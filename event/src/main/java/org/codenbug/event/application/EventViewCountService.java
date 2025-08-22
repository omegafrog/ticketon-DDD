package org.codenbug.event.ui;

import org.codenbug.event.ui.repository.EventViewRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트 조회수 관리를 위한 서비스
 * 비동기 처리를 통해 조회 성능에 영향을 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventViewCountService {
    
    private final EventViewRepository eventViewRepository;
    
    /**
     * 이벤트 조회수를 비동기적으로 증가
     * 메인 조회 로직과 분리하여 성능 영향 최소화
     */
    @Async("taskExecutor")
    @Transactional
    public void incrementViewCountAsync(String eventId) {
        try {
            eventViewRepository.incrementViewCount(eventId);
            log.debug("Successfully incremented view count for event: {}", eventId);
        } catch (Exception e) {
            // 조회수 증가 실패가 메인 로직에 영향주지 않도록 로깅만 수행
            log.warn("Failed to increment view count for event: {}, error: {}", eventId, e.getMessage());
        }
    }
    
    /**
     * 동기적 조회수 증가 (필요 시 사용)
     */
    @Transactional
    public void incrementViewCount(String eventId) {
        try {
            eventViewRepository.incrementViewCount(eventId);
            log.debug("Successfully incremented view count for event: {}", eventId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for event: {}, error: {}", eventId, e.getMessage());
            // 비즈니스 로직에 영향주지 않도록 예외를 다시 던지지 않음
        }
    }
}