package org.codenbug.purchase.app;

import org.codenbug.purchase.event.PaymentCompletedEvent;
import org.codenbug.purchase.event.RefundCompletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구매 관련 이벤트를 RabbitMQ로 발행하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final String PAYMENT_COMPLETED_QUEUE = "payment.completed";
    private static final String REFUND_COMPLETED_QUEUE = "refund.completed";

    /**
     * 결제 완료 이벤트 발행
     */
    public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(PAYMENT_COMPLETED_QUEUE, message);
            log.info("결제 완료 이벤트 발행 성공: userId={}, purchaseId={}", 
                event.getUserId(), event.getPurchaseId());
        } catch (Exception e) {
            log.error("결제 완료 이벤트 발행 실패: userId={}, purchaseId={}", 
                event.getUserId(), event.getPurchaseId(), e);
        }
    }

    /**
     * 환불 완료 이벤트 발행
     */
    public void publishRefundCompletedEvent(RefundCompletedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(REFUND_COMPLETED_QUEUE, message);
            log.info("환불 완료 이벤트 발행 성공: userId={}, purchaseId={}", 
                event.getUserId(), event.getPurchaseId());
        } catch (Exception e) {
            log.error("환불 완료 이벤트 발행 실패: userId={}, purchaseId={}", 
                event.getUserId(), event.getPurchaseId(), e);
        }
    }
}
