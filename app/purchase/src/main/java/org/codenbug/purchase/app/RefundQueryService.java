package org.codenbug.purchase.app;

import java.util.List;

import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundId;
import org.codenbug.purchase.domain.RefundRepository;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 환불 조회 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundQueryService {

    private final RefundRepository refundRepository;
    private final PurchaseService purchaseService;

    /**
     * 사용자별 환불 이력 조회
     */
    public Page<RefundDto> getUserRefundHistory(String userId, Pageable pageable) {
        UserId userIdVO = new UserId(userId);
        Page<Refund> refunds = refundRepository.findByPurchaseUserId(userIdVO, pageable);
        
        return refunds.map(this::convertToDto);
    }

    /**
     * 특정 구매의 환불 이력 조회
     */
    public List<RefundDto> getPurchaseRefundHistory(String purchaseId, String userId) {
        // 구매 정보와 권한 확인은 별도로 처리하거나 Purchase 조회를 통해 검증
        // 여기서는 간단히 환불 정보만 조회

        return refundRepository.findByPurchaseId(purchaseId) // 실제로는 Purchase 조회 필요
            .stream()
            .peek(refund -> refund.validateUserAccess(new UserId(userId)))
            .map(this::convertToDto)
            .toList();
    }

    /**
     * 환불 상태별 조회 (관리자용)
     */
    public List<RefundDto> getRefundsByStatus(RefundStatus status) {
        return refundRepository.findByStatus(status)
            .stream()
            .map(this::convertToDto)
            .toList();
    }

    /**
     * 특정 환불 상세 조회
     */
    public RefundDto getRefundDetail(String refundId, String userId) {
        RefundId refundIdVO = new RefundId(refundId);
        Refund refund = refundRepository.findById(refundIdVO)
            .orElseThrow(() -> new IllegalArgumentException("해당 환불 정보를 찾을 수 없습니다."));
        
        refund.validateUserAccess(new UserId(userId));
        
        return convertToDto(refund);
    }

    /**
     * Refund 엔티티를 DTO로 변환
     */
    private RefundDto convertToDto(Refund refund) {
        return RefundDto.builder()
            .refundId(refund.getRefundId().getValue())
            .purchaseId(refund.getPurchase().getPurchaseId().getValue())
            .refundAmount(refund.getRefundAmount().getValue())
            .refundReason(refund.getRefundReason().getValue())
            .status(refund.getStatus())
            .receiptUrl(refund.getReceiptUrl())
            .requestedAt(refund.getRequestedAt())
            .processedAt(refund.getProcessedAt())
            .isUserRefund(refund.isUserRefund())
            .isManagerRefund(refund.isManagerRefund())
            .isSystemRefund(refund.isSystemRefund())
            .build();
    }

    /**
     * 환불 조회용 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class RefundDto {
        private final String refundId;
        private final String purchaseId;
        private final Integer refundAmount;
        private final String refundReason;
        private final RefundStatus status;
        private final String receiptUrl;
        private final java.time.LocalDateTime requestedAt;
        private final java.time.LocalDateTime processedAt;
        private final boolean isUserRefund;
        private final boolean isManagerRefund;
        private final boolean isSystemRefund;
        
        public String getFormattedRefundAmount() {
            return String.format("%,d원", refundAmount);
        }
        
        public String getStatusDescription() {
            return status.getDescription();
        }
        
        public String getRefundTypeDescription() {
            if (isUserRefund) return "사용자 요청";
            if (isManagerRefund) return "매니저 처리";
            if (isSystemRefund) return "시스템 자동";
            return "알 수 없음";
        }
    }
}