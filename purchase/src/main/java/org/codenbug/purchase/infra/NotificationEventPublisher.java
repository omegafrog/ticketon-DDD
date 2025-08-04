package org.codenbug.purchase.infra;

import org.codenbug.purchase.event.ManagerRefundCompletedEvent;
import org.codenbug.purchase.event.RefundCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 관련 이벤트를 Kafka로 발행하는 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String REFUND_COMPLETED_TOPIC = "notification.refund.completed";
    private static final String MANAGER_REFUND_COMPLETED_TOPIC = "notification.manager.refund.completed";

    /**
     * 사용자 환불 완료 이벤트 발행
     */
    public void publishRefundCompletedEvent(RefundCompletedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(REFUND_COMPLETED_TOPIC, event.getUserId(), message);
            log.info("환불 완료 이벤트 발행 성공: userId={}, purchaseId={}, refundAmount={}", 
                event.getUserId(), event.getPurchaseId(), event.getRefundAmount());
        } catch (Exception e) {
            log.error("환불 완료 이벤트 발행 실패: userId={}, purchaseId={}", 
                event.getUserId(), event.getPurchaseId(), e);
        }
    }

    /**
     * 매니저 환불 완료 이벤트 발행
     */
    public void publishManagerRefundCompletedEvent(ManagerRefundCompletedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(MANAGER_REFUND_COMPLETED_TOPIC, event.getUserId(), message);
            log.info("매니저 환불 완료 이벤트 발행 성공: userId={}, purchaseId={}, managerName={}", 
                event.getUserId(), event.getPurchaseId(), event.getManagerName());
        } catch (Exception e) {
            log.error("매니저 환불 완료 이벤트 발행 실패: userId={}, purchaseId={}", 
                event.getUserId(), event.getPurchaseId(), e);
        }
    }
}