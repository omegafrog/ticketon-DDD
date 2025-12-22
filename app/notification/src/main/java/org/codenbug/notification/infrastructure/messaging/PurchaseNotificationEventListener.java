package org.codenbug.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.notification.application.service.NotificationApplicationService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.springframework.stereotype.Component;

/**
 * 구매 관련 이벤트를 수신하여 알림을 생성하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseNotificationEventListener {

    private final NotificationApplicationService notificationApplicationService;
    private final ObjectMapper objectMapper;

    /**
     * 환불 완료 이벤트 수신 및 알림 생성
     */
    //@KafkaListener(topics = "notification.refund.completed", groupId = "app-notification-service")
    public void handleRefundCompletedEvent(String message) {
        try {
            RefundCompletedEventDto event =
                objectMapper.readValue(message, RefundCompletedEventDto.class);

            String title = "[티켓온] 환불 완료";
            String content = String.format(
                "주문번호: %s\n공연명: %s\n환불 금액: %s원\n환불 사유: %s\n\n환불이 완료되었습니다. 환불 금액은 결제 수단에 따라 3~7일 내에 처리됩니다.",
                event.getOrderId(),
                event.getEventName() != null ? event.getEventName() : event.getOrderName(),
                String.format("%,d", event.getRefundAmount()), event.getRefundReason());
            String targetUrl = "/my-account/refund-history";

            notificationApplicationService.createNotification(event.getUserId(),
                NotificationType.PAYMENT, title, content, targetUrl);

            log.info("환불 완료 이벤트 처리 및 알림 생성 완료: userId={}, purchaseId={}, refundAmount={}",
                event.getUserId(), event.getPurchaseId(), event.getRefundAmount());

        } catch (Exception e) {
            log.error("환불 완료 이벤트 처리 실패: message={}", message, e);
        }
    }

    /**
     * 매니저 환불 완료 이벤트 수신 및 알림 생성
     */
    //@KafkaListener(topics = "notification.manager.refund.completed",
//    groupId ="app-notification-service")
    public void handleManagerRefundCompletedEvent(String message) {
        try {
            ManagerRefundCompletedEventDto event =
                objectMapper.readValue(message, ManagerRefundCompletedEventDto.class);

            String title = "[티켓온] 매니저 환불 처리";
            String content = String.format(
                "주문번호: %s\n공연명: %s\n환불 금액: %s원\n환불 사유: %s\n처리자: %s\n\n매니저에 의해 환불이 처리되었습니다. 환불 금액은 결제 수단에 따라 3~7일 내에 처리됩니다.",
                event.getOrderId(),
                event.getEventName() != null ? event.getEventName() : event.getOrderName(),
                String.format("%,d", event.getRefundAmount()), event.getRefundReason(),
                event.getManagerName());
            String targetUrl = "/my-account/refund-history";

            notificationApplicationService.createNotification(event.getUserId(),
                NotificationType.PAYMENT, title, content, targetUrl);

            log.info("매니저 환불 완료 이벤트 처리 및 알림 생성 완료: userId={}, purchaseId={}, managerName={}",
                event.getUserId(), event.getPurchaseId(), event.getManagerName());

        } catch (Exception e) {
            log.error("매니저 환불 완료 이벤트 처리 실패: message={}", message, e);
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
        private String eventName;

        public RefundCompletedEventDto() {
        }

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

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }
    }

    /**
     * 매니저 환불 완료 이벤트 DTO (내부 클래스)
     */
    public static class ManagerRefundCompletedEventDto {

        private String userId;
        private String purchaseId;
        private String orderId;
        private String orderName;
        private Integer refundAmount;
        private String refundReason;
        private String refundedAt;
        private String eventName;
        private String managerName;

        public ManagerRefundCompletedEventDto() {
        }

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

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public String getManagerName() {
            return managerName;
        }

        public void setManagerName(String managerName) {
            this.managerName = managerName;
        }
    }
}
