package org.codenbug.purchase.app;

import java.util.List;

import org.codenbug.purchase.domain.MessagePublisher;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundDomainService;
import org.codenbug.purchase.domain.RefundRepository;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.event.ManagerRefundCompletedEvent;
import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.codenbug.purchase.infra.NotificationEventPublisher;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매니저 환불 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManagerRefundService {

    private final PGApiService pgApiService;
    private final PurchaseRepository purchaseRepository;
    private final RefundRepository refundRepository;
    private final RefundDomainService refundDomainService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final MessagePublisher messagePublisher;

    /**
     * 매니저에 의한 단일 구매 환불
     */
    public ManagerRefundResult processManagerRefund(String purchaseId, String refundReason, 
                                                   String managerName, UserId managerId) {
        log.info("매니저 환불 처리 시작: purchaseId={}, managerId={}", purchaseId, managerId.getValue());

        // 1. 구매 정보 조회
        Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
            .orElseThrow(() -> new IllegalArgumentException("해당 구매 정보를 찾을 수 없습니다."));

        // 2. 환불 가능 여부 확인
        if (!purchase.canRefund()) {
            throw new IllegalStateException("환불할 수 없는 구매입니다: " + purchase.getPaymentStatus());
        }

        // 3. 외부 결제 시스템 환불 요청 (전액 환불)
        String paymentKey = purchase.getPaymentKey();
        CanceledPaymentInfo canceledPaymentInfo = pgApiService.cancelPayment(paymentKey, 
            "매니저 환불: " + refundReason);

        // 4. 환불 금액 계산
        int refundAmount = canceledPaymentInfo.getCancels().stream()
            .mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
            .sum();

        // 5. 도메인 서비스를 통한 매니저 환불 처리
        RefundDomainService.RefundResult refundResult = refundDomainService.processManagerRefund(
            purchase, refundAmount, refundReason, managerId);

        // 6. 환불 엔티티 저장 및 완료 처리
        Refund savedRefund = refundRepository.save(refundResult.getRefund());
        refundDomainService.completeRefundWithPaymentInfo(savedRefund, canceledPaymentInfo);
        refundRepository.save(savedRefund);

        // 7. 구매 정보 저장
        purchaseRepository.save(purchase);

        // 8. 매니저 환불 알림 이벤트 발행
        publishManagerRefundEvent(purchase, refundAmount, refundReason, managerName, canceledPaymentInfo);

        // 9. 좌석 취소 이벤트 발행
        messagePublisher.publishSeatPurchaseCanceledEvent(refundResult.getSeatIds(), purchaseId);

        log.info("매니저 환불 처리 완료: purchaseId={}, refundId={}", 
            purchaseId, savedRefund.getRefundId().getValue());

        return ManagerRefundResult.builder()
            .refund(savedRefund)
            .purchase(purchase)
            .refundAmount(refundAmount)
            .seatIds(refundResult.getSeatIds())
            .build();
    }

    /**
     * 특정 이벤트의 모든 구매에 대한 일괄 환불 (공연 취소 등)
     */
    public List<ManagerRefundResult> processBatchRefund(String eventId, String refundReason, 
                                                      String managerName, UserId managerId) {
        log.info("일괄 환불 처리 시작: eventId={}, managerId={}", eventId, managerId.getValue());

        // 해당 이벤트의 모든 완료된 구매 조회
        List<Purchase> purchases = purchaseRepository.findByEventIdAndPaymentStatus(eventId, PaymentStatus.DONE);
        
        return purchases.stream()
            .map(purchase -> {
                try {
                    return processManagerRefund(
                        purchase.getPurchaseId().getValue(), 
                        refundReason, 
                        managerName, 
                        managerId
                    );
                } catch (Exception e) {
                    log.error("일괄 환불 중 개별 환불 실패: purchaseId={}, error={}", 
                        purchase.getPurchaseId().getValue(), e.getMessage());
                    // 개별 실패는 전체 처리를 중단하지 않음
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    /**
     * 매니저 환불 알림 이벤트 발행
     */
    private void publishManagerRefundEvent(Purchase purchase, int refundAmount, String refundReason, 
                                         String managerName, CanceledPaymentInfo canceledPaymentInfo) {
        try {
            ManagerRefundCompletedEvent event = ManagerRefundCompletedEvent.of(
                purchase.getUserId().getValue(),
                purchase.getPurchaseId().getValue(),
                purchase.getOrderId(),
                purchase.getOrderName(),
                refundAmount,
                refundReason,
                canceledPaymentInfo.getCancels().get(0).getCanceledAt(),
                purchase.getOrderName(),
                managerName
            );

            notificationEventPublisher.publishManagerRefundCompletedEvent(event);
        } catch (Exception e) {
            log.error("매니저 환불 알림 이벤트 발행 실패: purchaseId={}, managerName={}", 
                purchase.getPurchaseId().getValue(), managerName, e);
        }
    }

    /**
     * 매니저 환불 결과 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ManagerRefundResult {
        private final Refund refund;
        private final Purchase purchase;
        private final int refundAmount;
        private final List<String> seatIds;
        
        public String getRefundId() {
            return refund.getRefundId().getValue();
        }
        
        public String getPurchaseId() {
            return purchase.getPurchaseId().getValue();
        }
        
        public String getUserId() {
            return purchase.getUserId().getValue();
        }
    }
}