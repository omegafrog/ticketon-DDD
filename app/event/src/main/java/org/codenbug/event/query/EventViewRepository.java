package org.codenbug.event.query;

import org.codenbug.event.application.dto.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 이벤트 뷰 조회 전용 Repository Projection을 사용하여 N+1 문제 없이 최적화된 쿼리 수행
 */
public interface EventViewRepository {

    /**
     * 이벤트 리스트 조회 (필터링 + 페이징) 필요한 컬럼만 SELECT하여 성능 최적화, Redis viewCount 포함
     */
    Page<EventListProjection> findEventList(String keyword, EventListFilter filter,
        Pageable pageable);

    /**
     * 매니저별 이벤트 리스트 조회 (Redis viewCount 포함)
     */
    Page<EventListProjection> findManagerEventList(String managerId, Pageable pageable);

    /**
     * 커서 기반 페이징으로 이벤트 리스트 조회 대용량 데이터 처리 시 성능 향상, Redis viewCount 포함
     */
    Page<EventListProjection> findEventListWithCursor(String keyword, EventListFilter filter,
        String lastEventId, int size);

    /**
     * 이벤트 단건 조회 (Redis viewCount 포함)
     */
    EventListProjection findEventById(String eventId);

    /**
     * 이벤트 조회수 증가 비동기적으로 처리하여 조회 성능에 영향 최소화
     */
    void incrementViewCount(String eventId);
}