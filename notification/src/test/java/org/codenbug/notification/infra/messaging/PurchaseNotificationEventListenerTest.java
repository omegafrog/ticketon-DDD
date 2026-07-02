package org.codenbug.notification.infra.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.codenbug.notification.application.NotificationCommandService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.ui.dto.NotificationDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

class PurchaseNotificationEventListenerTest {

    private final NotificationCommandService notificationCommandService =
            org.mockito.Mockito.mock(NotificationCommandService.class);
    private final PurchaseNotificationEventListener listener =
            new PurchaseNotificationEventListener(notificationCommandService, new ObjectMapper());

    @Test
    void 매니저_환불_이벤트는_managerName까지_포함한_sourceKey를_사용한다() {
        when(notificationCommandService.createNotificationIfAbsent(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new NotificationDto()));

        listener.handleManagerRefundCompletedEvent("""
                {
                  "userId": "user-1",
                  "purchaseId": "purchase-1",
                  "orderId": "order-1",
                  "orderName": "Concert",
                  "eventName": "Concert",
                  "refundAmount": 1000,
                  "refundReason": "reason",
                  "refundedAt": "2026-06-19T10:15:30",
                  "managerName": "manager-1"
                }
                """);

        verify(notificationCommandService).createNotificationIfAbsent(eq("user-1"),
                eq(NotificationType.PAYMENT), eq("[티켓온] 매니저 환불 처리"), any(),
                eq("/my-account/refund-history"),
                eq("notification.manager.refund.completed:user-1:purchase-1:2026-06-19T10:15:30:manager-1"));
    }

    @Test
    void 매니저_환불_이벤트는_content에_관리자와_환불정보를_채운다() {
        when(notificationCommandService.createNotificationIfAbsent(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new NotificationDto()));

        listener.handleManagerRefundCompletedEvent("""
                {
                  "userId": "user-1",
                  "purchaseId": "purchase-1",
                  "orderId": "order-1",
                  "orderName": "Concert",
                  "eventName": "Concert",
                  "refundAmount": 1000,
                  "refundReason": "reason",
                  "refundedAt": "2026-06-19T10:15:30",
                  "managerName": "manager-1"
                }
                """);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationCommandService).createNotificationIfAbsent(eq("user-1"),
                eq(NotificationType.PAYMENT), eq("[티켓온] 매니저 환불 처리"),
                contentCaptor.capture(), eq("/my-account/refund-history"),
                eq("notification.manager.refund.completed:user-1:purchase-1:2026-06-19T10:15:30:manager-1"));
        org.assertj.core.api.Assertions.assertThat(contentCaptor.getValue()).contains("처리자: manager-1",
                "환불 금액: 1,000원", "환불 사유: reason");
    }
}
