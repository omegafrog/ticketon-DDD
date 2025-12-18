package org.codenbug.purchase.domain;

import java.util.List;

import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 환불 도메인 서비스
 * 복잡한 환불 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundDomainService {

    /**
     * 사용자 환불 요청 처리
     */
    public RefundResult processUserRefund(Purchase purchase, Integer refundAmount, String reason, UserId userId) {
        log.info("사용자 환불 요청 처리 시작: purchaseId={}, userId={}, amount={}", 
            purchase.getPurchaseId().getValue(), userId.getValue(), refundAmount);
        
        // 1. 환불 가능 여부 검증
        validateRefundRequest(purchase, refundAmount, userId);
        
        // 2. 환불 엔티티 생성
        Refund refund = Refund.createUserRefund(purchase, refundAmount, reason, userId);
        
        // 3. 구매 엔티티 상태 업데이트
        if (isFullRefund(purchase, refundAmount)) {
            purchase.markAsRefunded();
        } else {
            purchase.markAsPartialRefunded();
        }
        
        // 4. 티켓 처리 (좌석 해제)
        List<String> seatIds = purchase.getTickets().stream()
            .map(Ticket::getSeatId)
            .toList();
        
        purchase.getTickets().clear();
        
        log.info("사용자 환불 요청 처리 완료: refundId={}", refund.getRefundId().getValue());
        
        return RefundResult.builder()
            .refund(refund)
            .seatIds(seatIds)
            .isFullRefund(isFullRefund(purchase, refundAmount))
            .build();
    }

    /**
     * 매니저 환불 요청 처리
     */
    public RefundResult processManagerRefund(Purchase purchase, Integer refundAmount, String reason, UserId managerId) {
        log.info("매니저 환불 요청 처리 시작: purchaseId={}, managerId={}, amount={}", 
            purchase.getPurchaseId().getValue(), managerId.getValue(), refundAmount);
        
        // 1. 기본 검증 (사용자 권한 검증은 제외)
        validateRefundAmount(purchase, refundAmount);
        
        // 2. 매니저 환불 엔티티 생성
        Refund refund = Refund.createManagerRefund(purchase, refundAmount, reason, managerId);
        
        // 3. 구매 엔티티 상태 업데이트
        if (isFullRefund(purchase, refundAmount)) {
            purchase.markAsRefunded();
        } else {
            purchase.markAsPartialRefunded();
        }
        
        // 4. 티켓 처리
        List<String> seatIds = purchase.getTickets().stream()
            .map(Ticket::getSeatId)
            .toList();
        
        purchase.getTickets().clear();
        
        log.info("매니저 환불 요청 처리 완료: refundId={}", refund.getRefundId().getValue());
        
        return RefundResult.builder()
            .refund(refund)
            .seatIds(seatIds)
            .isFullRefund(isFullRefund(purchase, refundAmount))
            .build();
    }

    /**
     * 시스템 자동 환불 처리 (공연 취소 등)
     */
    public RefundResult processSystemRefund(Purchase purchase, String reason) {
        log.info("시스템 자동 환불 처리 시작: purchaseId={}", purchase.getPurchaseId().getValue());
        
        // 전액 환불
        Integer refundAmount = purchase.getTotalAmount();
        
        // 1. 기본 검증
        if (!purchase.canRefund()) {
            throw new IllegalStateException("환불할 수 없는 구매입니다.");
        }
        
        // 2. 시스템 환불 엔티티 생성
        Refund refund = Refund.createSystemRefund(purchase, refundAmount, reason);
        
        // 3. 구매 엔티티 상태 업데이트
        purchase.markAsRefunded();
        
        // 4. 티켓 처리
        List<String> seatIds = purchase.getTickets().stream()
            .map(Ticket::getSeatId)
            .toList();
        
        purchase.getTickets().clear();
        
        log.info("시스템 자동 환불 처리 완료: refundId={}", refund.getRefundId().getValue());
        
        return RefundResult.builder()
            .refund(refund)
            .seatIds(seatIds)
            .isFullRefund(true)
            .build();
    }

    /**
     * 외부 결제 시스템 환불 완료 처리
     */
    public void completeRefundWithPaymentInfo(Refund refund, CanceledPaymentInfo paymentInfo) {
        log.info("외부 결제 시스템 환불 완료 처리: refundId={}", refund.getRefundId().getValue());
        
        if (paymentInfo.getCancels() != null && !paymentInfo.getCancels().isEmpty()) {
            CanceledPaymentInfo.CancelDetail cancelDetail = paymentInfo.getCancels().get(0);
            
            String receiptUrl = paymentInfo.getReceipt() != null ? 
                paymentInfo.getReceipt().getUrl() : null;
            
            refund.completeRefund(paymentInfo.getPaymentKey(), receiptUrl);
        } else {
            refund.failRefund("결제 취소 정보가 없습니다.");
        }
    }

    /**
     * 환불 요청 검증
     */
    private void validateRefundRequest(Purchase purchase, Integer refundAmount, UserId userId) {
        if (!purchase.canRefund()) {
            throw new IllegalStateException("환불할 수 없는 구매입니다.");
        }
        
        purchase.validate(purchase.getOrderId(), purchase.getTotalAmount(), userId.getValue());
        validateRefundAmount(purchase, refundAmount);
    }

    /**
     * 환불 금액 검증
     */
    private void validateRefundAmount(Purchase purchase, Integer refundAmount) {
        purchase.validateRefundAmount(refundAmount);
    }

    /**
     * 전액 환불 여부 확인
     */
    private boolean isFullRefund(Purchase purchase, Integer refundAmount) {
        return refundAmount.equals(purchase.getTotalAmount());
    }

    /**
     * 환불 결과 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class RefundResult {
        private final Refund refund;
        private final List<String> seatIds;
        private final boolean isFullRefund;
    }
}