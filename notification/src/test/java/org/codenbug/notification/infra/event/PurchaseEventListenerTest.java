package org.codenbug.notification.infra.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.codenbug.notification.application.NotificationCommandService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.ui.dto.NotificationDto;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class PurchaseEventListenerTest {

    private final NotificationCommandService notificationCommandService =
            org.mockito.Mockito.mock(NotificationCommandService.class);
    private final PurchaseEventListener listener =
            new PurchaseEventListener(notificationCommandService, new ObjectMapper());

    @Test
    void 결제완료_이벤트는_결정적_sourceKey로_멱등생성을_호출한다() {
        when(notificationCommandService.createNotificationIfAbsent(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new NotificationDto()));

        listener.handlePaymentCompletedEvent("""
                {
                  "userId": "user-1",
                  "purchaseId": "purchase-1",
                  "orderId": "order-1",
                  "eventTitle": "Concert",
                  "totalAmount": 1000,
                  "paymentMethod": "CARD",
                  "approvedAt": "2026-06-19T10:15:30"
                }
                """);

        verify(notificationCommandService).createNotificationIfAbsent(eq("user-1"),
                eq(NotificationType.PAYMENT), eq("[티켓온] 결제 완료"), any(), eq("/purchase-history/purchase-1"),
                eq("payment.completed:user-1:purchase-1:2026-06-19T10:15:30"));
    }

    @Test
    void stable_key_필드가_없으면_저장하지_않고_건너뛴다() {
        listener.handleRefundCompletedEvent("""
                {
                  "userId": "user-1",
                  "purchaseId": "",
                  "orderId": "order-1",
                  "orderName": "Concert",
                  "refundAmount": 1000,
                  "refundReason": "reason",
                  "refundedAt": "2026-06-19T10:15:30"
                }
                """);

        verify(notificationCommandService, never()).createNotificationIfAbsent(any(), any(), any(),
                any(), any(), any());
    }
}
