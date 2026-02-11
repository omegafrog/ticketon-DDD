package org.codenbug.event.application;

import org.codenbug.event.query.EventViewRepository;
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
    
}