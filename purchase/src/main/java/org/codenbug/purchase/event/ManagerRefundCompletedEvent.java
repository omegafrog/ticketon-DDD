package org.codenbug.purchase.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매니저에 의한 환불 완료 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerRefundCompletedEvent {
    private String userId;
    private String purchaseId;
    private String orderId;
    private String orderName;
    private Integer refundAmount;
    private String refundReason;
    private String refundedAt;
    private String eventName;
    private String managerName;
    
    public static ManagerRefundCompletedEvent of(String userId, String purchaseId, String orderId,
                                               String orderName, Integer refundAmount, String refundReason,
                                               String refundedAt, String eventName, String managerName) {
        return ManagerRefundCompletedEvent.builder()
                .userId(userId)
                .purchaseId(purchaseId)
                .orderId(orderId)
                .orderName(orderName)
                .refundAmount(refundAmount)
                .refundReason(refundReason)
                .refundedAt(refundedAt)
                .eventName(eventName)
                .managerName(managerName)
                .build();
    }
}