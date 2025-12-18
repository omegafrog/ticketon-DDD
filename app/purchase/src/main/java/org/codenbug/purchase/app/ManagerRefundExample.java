package org.codenbug.purchase.app;

import org.codenbug.purchase.event.ManagerRefundCompletedEvent;
import org.codenbug.purchase.infra.NotificationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매니저 환불 기능 예시 코드
 * 실제 매니저 환불 기능 구현 시 참고용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManagerRefundExample {

    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 매니저 환불 처리 시 알림 이벤트 발행 예시
     */
    public void publishManagerRefundNotification(String userId, String purchaseId, String orderId, 
                                               String orderName, int refundAmount, String refundReason,
                                               String refundedAt, String eventName, String managerName) {
        try {
            ManagerRefundCompletedEvent managerRefundEvent = ManagerRefundCompletedEvent.of(
                userId,
                purchaseId,
                orderId,
                orderName,
                refundAmount,
                refundReason,
                refundedAt,
                eventName,
                managerName
            );

            notificationEventPublisher.publishManagerRefundCompletedEvent(managerRefundEvent);
            
            log.info("매니저 환불 알림 이벤트 발행 성공: userId={}, purchaseId={}, managerName={}", 
                userId, purchaseId, managerName);
        } catch (Exception e) {
            log.error("매니저 환불 알림 이벤트 발행 실패: userId={}, purchaseId={}, managerName={}", 
                userId, purchaseId, managerName, e);
        }
    }

    /**
     * 실제 매니저 환불 메서드에서 사용할 수 있는 코드 조각
     */
    private void sendManagerRefundNotification(/* Purchase purchase, ManagerRefundRequest request, 
                                              CanceledPaymentInfo canceledPaymentInfo, String managerName */) {
        /*
        // 매니저 환불 알림 이벤트 발행
        try {
            int refundAmount = canceledPaymentInfo.getCancels().stream()
                .mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
                .sum();

            ManagerRefundCompletedEvent managerRefundEvent = ManagerRefundCompletedEvent.of(
                purchase.getUserId().getValue(),
                purchase.getPurchaseId().getValue(),
                purchase.getOrderId(),
                purchase.getOrderName(),
                refundAmount,
                request.getReason(),
                canceledPaymentInfo.getCancels().get(0).getCanceledAt(),
                purchase.getOrderName(), // eventName 대신 orderName 사용
                managerName
            );

            notificationEventPublisher.publishManagerRefundCompletedEvent(managerRefundEvent);
        } catch (Exception e) {
            log.error("매니저 환불 알림 이벤트 발행 실패. 사용자ID: {}, 구매ID: {}, 매니저: {}, 오류: {}",
                purchase.getUserId().getValue(), purchase.getPurchaseId().getValue(), managerName, e.getMessage(), e);
            // 알림 이벤트 발행 실패는 환불 처리에 영향을 주지 않도록 예외를 무시함
        }
        */
    }
}