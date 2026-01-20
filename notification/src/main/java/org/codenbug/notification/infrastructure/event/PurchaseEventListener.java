package org.codenbug.notification.infrastructure.event;

import org.codenbug.notification.application.service.NotificationApplicationService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구매 관련 이벤트를 수신하여 알림을 생성하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseEventListener {

    private final NotificationApplicationService notificationApplicationService;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트 수신 및 알림 생성
     */
    @RabbitListener(queues = "payment.completed")
    public void handlePaymentCompletedEvent(String message) {
        try {
            PaymentCompletedEventDto event =
                    objectMapper.readValue(message, PaymentCompletedEventDto.class);

            String title = "[티켓온] 결제 완료";
            String content =
                    String.format("주문번호: %s\n공연명: %s\n결제 금액: %d원\n결제 수단: %s\n결제가 성공적으로 완료되었습니다.",
                            event.getOrderId(), event.getEventTitle(), event.getTotalAmount(),
                            event.getPaymentMethod());
            String targetUrl = String.format("/purchase-history/%s", event.getPurchaseId());

            notificationApplicationService.createNotification(event.getUserId(),
                    NotificationType.PAYMENT, title, content, targetUrl);

            log.info("결제 완료 이벤트 처리 및 알림 생성 완료: userId={}, purchaseId={}", event.getUserId(),
                    event.getPurchaseId());

        } catch (Exception e) {
            log.error("결제 완료 이벤트 처리 실패: message={}", message, e);
        }
    }

    /**
     * 환불 완료 이벤트 수신 및 알림 생성
     */
    @RabbitListener(queues = "refund.completed")
    public void handleRefundCompletedEvent(String message) {
        try {
            RefundCompletedEventDto event =
                    objectMapper.readValue(message, RefundCompletedEventDto.class);

            String title = "[티켓온] 환불 완료";
            String content = String.format("주문번호: %s\n주문명: %s\n환불 금액: %d원\n환불 사유: %s\n환불이 완료되었습니다.",
                    event.getOrderId(), event.getOrderName(), event.getRefundAmount(),
                    event.getRefundReason());
            String targetUrl = "/my-account/refund-history";

            notificationApplicationService.createNotification(event.getUserId(),
                    NotificationType.PAYMENT, title, content, targetUrl);

            log.info("환불 완료 이벤트 처리 및 알림 생성 완료: userId={}, purchaseId={}", event.getUserId(),
                    event.getPurchaseId());

        } catch (Exception e) {
            log.error("환불 완료 이벤트 처리 실패: message={}", message, e);
        }
    }

    /**
     * 결제 완료 이벤트 DTO (내부 클래스)
     */
    public static class PaymentCompletedEventDto {
        private String userId;
        private String purchaseId;
        private String orderId;
        private String orderName;
        private Integer totalAmount;
        private String eventTitle;
        private String paymentMethod;
        private String approvedAt;

        public PaymentCompletedEventDto() {}

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPurchaseId() {
            return purchaseId;
        }

        public void setPurchaseId(String purchaseId) {
            this.purchaseId = purchaseId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getOrderName() {
            return orderName;
        }

        public void setOrderName(String orderName) {
            this.orderName = orderName;
        }

        public Integer getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Integer totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getEventTitle() {
            return eventTitle;
        }

        public void setEventTitle(String eventTitle) {
            this.eventTitle = eventTitle;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getApprovedAt() {
            return approvedAt;
        }

        public void setApprovedAt(String approvedAt) {
            this.approvedAt = approvedAt;
        }
    }

    /**
     * 환불 완료 이벤트 DTO (내부 클래스)
     */
    public static class RefundCompletedEventDto {
        private String userId;
        private String purchaseId;
        private String orderId;
        private String orderName;
        private Integer refundAmount;
        private String refundReason;
        private String refundedAt;

        public RefundCompletedEventDto() {}

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPurchaseId() {
            return purchaseId;
        }

        public void setPurchaseId(String purchaseId) {
            this.purchaseId = purchaseId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getOrderName() {
            return orderName;
        }

        public void setOrderName(String orderName) {
            this.orderName = orderName;
        }

        public Integer getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(Integer refundAmount) {
            this.refundAmount = refundAmount;
        }

        public String getRefundReason() {
            return refundReason;
        }

        public void setRefundReason(String refundReason) {
            this.refundReason = refundReason;
        }

        public String getRefundedAt() {
            return refundedAt;
        }

        public void setRefundedAt(String refundedAt) {
            this.refundedAt = refundedAt;
        }
    }
}
