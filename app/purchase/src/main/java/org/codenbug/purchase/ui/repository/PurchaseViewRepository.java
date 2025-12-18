package org.codenbug.purchase.ui.repository;

import org.codenbug.purchase.ui.projection.PurchaseListProjection;
import org.codenbug.purchase.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구매 뷰 조회 전용 Repository
 * Projection을 사용하여 N+1 문제 없이 최적화된 쿼리 수행
 */
public interface PurchaseViewRepository {
    
    /**
     * 사용자별 구매 리스트 조회 (상태별 필터링)
     * Ticket 정보를 JOIN으로 한 번에 가져와서 N+1 문제 해결
     */
    Page<PurchaseListProjection> findUserPurchaseList(String userId, List<PaymentStatus> statuses, Pageable pageable);
    
    /**
     * 이벤트별 구매 리스트 조회
     */
    Page<PurchaseListProjection> findEventPurchaseList(String eventId, PaymentStatus paymentStatus, Pageable pageable);
    
    /**
     * 커서 기반 페이징으로 구매 리스트 조회
     */
    List<PurchaseListProjection> findUserPurchaseListWithCursor(String userId, List<PaymentStatus> statuses, 
                                                               LocalDateTime cursor, int size);
}