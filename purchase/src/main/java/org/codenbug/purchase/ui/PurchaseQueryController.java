package org.codenbug.purchase.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.ui.projection.PurchaseListProjection;
import org.codenbug.purchase.ui.repository.PurchaseViewRepository;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 구매 내역 조회 전용 Controller
 * N+1 문제 해결을 위한 Projection 기반 최적화 구현
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/purchases")
public class PurchaseQueryController {
    
    private final PurchaseViewRepository purchaseViewRepository;
    
    /**
     * 사용자별 구매 내역 조회
     * Ticket 정보를 포함하여 한 번의 최적화된 쿼리로 조회
     */
    @GetMapping("/history")
    @AuthNeeded
    @RoleRequired(Role.USER)
    public ResponseEntity<RsData<Page<PurchaseListProjection>>> getPurchaseHistory(
            @RequestParam(required = false, defaultValue = "DONE,EXPIRED") List<String> statuses,
            Pageable pageable) {
        
        String userId = LoggedInUserContext.get().getUserId();
        
        // String을 PaymentStatus로 변환
        List<PaymentStatus> paymentStatuses = statuses.stream()
            .map(PaymentStatus::valueOf)
            .toList();
        
        // 최적화된 Projection 조회로 N+1 문제 해결
        Page<PurchaseListProjection> purchaseHistory = purchaseViewRepository
            .findUserPurchaseList(userId, paymentStatuses, pageable);
        
        return ResponseEntity.ok(new RsData<>("200", "구매 내역 조회 성공", purchaseHistory));
    }
    
    /**
     * 이벤트별 구매 내역 조회 (매니저용)
     */
    @GetMapping("/event/{eventId}")
    @AuthNeeded
    @RoleRequired(Role.MANAGER)
    public ResponseEntity<RsData<Page<PurchaseListProjection>>> getEventPurchases(
            @PathVariable String eventId,
            @RequestParam(required = false, defaultValue = "DONE") String status,
            Pageable pageable) {
        
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
        
        // 최적화된 Projection 조회
        Page<PurchaseListProjection> eventPurchases = purchaseViewRepository
            .findEventPurchaseList(eventId, paymentStatus, pageable);
        
        return ResponseEntity.ok(new RsData<>("200", "이벤트 구매 내역 조회 성공", eventPurchases));
    }
}