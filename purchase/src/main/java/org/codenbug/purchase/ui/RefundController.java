package org.codenbug.purchase.ui;

import java.util.List;

import org.codenbug.purchase.app.ManagerRefundService;
import org.codenbug.purchase.app.RefundQueryService;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 환불 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundQueryService refundQueryService;
    private final ManagerRefundService managerRefundService;

    /**
     * 사용자 환불 이력 조회
     */
    @GetMapping("/my")
    public ResponseEntity<Page<RefundQueryService.RefundDto>> getMyRefunds(
            @RequestParam String userId,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<RefundQueryService.RefundDto> refunds = refundQueryService.getUserRefundHistory(userId, pageable);
        return ResponseEntity.ok(refunds);
    }

    /**
     * 특정 환불 상세 조회
     */
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundQueryService.RefundDto> getRefundDetail(
            @PathVariable String refundId,
            @RequestParam String userId) {
        
        RefundQueryService.RefundDto refund = refundQueryService.getRefundDetail(refundId, userId);
        return ResponseEntity.ok(refund);
    }

    /**
     * 매니저 단일 환불 처리
     */
    @PostMapping("/manager/single")
    public ResponseEntity<ManagerRefundService.ManagerRefundResult> processManagerRefund(
            @RequestBody ManagerRefundRequest request) {
        
        ManagerRefundService.ManagerRefundResult result = managerRefundService.processManagerRefund(
            request.getPurchaseId(),
            request.getRefundReason(),
            request.getManagerName(),
            new UserId(request.getManagerId())
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 매니저 일괄 환불 처리 (이벤트 취소 등)
     */
    @PostMapping("/manager/batch")
    public ResponseEntity<List<ManagerRefundService.ManagerRefundResult>> processBatchRefund(
            @RequestBody BatchRefundRequest request) {
        
        List<ManagerRefundService.ManagerRefundResult> results = managerRefundService.processBatchRefund(
            request.getEventId(),
            request.getRefundReason(),
            request.getManagerName(),
            new UserId(request.getManagerId())
        );
        
        return ResponseEntity.ok(results);
    }

    /**
     * 환불 상태별 조회 (관리자용)
     */
    @GetMapping("/admin/by-status")
    public ResponseEntity<List<RefundQueryService.RefundDto>> getRefundsByStatus(
            @RequestParam RefundStatus status) {
        
        List<RefundQueryService.RefundDto> refunds = refundQueryService.getRefundsByStatus(status);
        return ResponseEntity.ok(refunds);
    }

    /**
     * 매니저 환불 요청 DTO
     */
    public static class ManagerRefundRequest {
        private String purchaseId;
        private String refundReason;
        private String managerId;
        private String managerName;

        // Getters and Setters
        public String getPurchaseId() { return purchaseId; }
        public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }
        
        public String getRefundReason() { return refundReason; }
        public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
        
        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
        
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
    }

    /**
     * 일괄 환불 요청 DTO
     */
    public static class BatchRefundRequest {
        private String eventId;
        private String refundReason;
        private String managerId;
        private String managerName;

        // Getters and Setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        
        public String getRefundReason() { return refundReason; }
        public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
        
        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
        
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
    }
}