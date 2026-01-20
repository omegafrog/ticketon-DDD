package org.codenbug.purchase.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 완료 이벤트 RabbitMQ를 통해 다른 서비스에게 결제 완료를 알리는 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private String userId;
    private String purchaseId;
    private String orderId;
    private String orderName;
    private Integer totalAmount;
    private String eventTitle;
    private String paymentMethod;
    private String approvedAt;

    public static PaymentCompletedEvent of(String userId, String purchaseId, String orderId,
            String orderName, Integer totalAmount, String eventTitle, String paymentMethod,
            String approvedAt) {
        return new PaymentCompletedEvent(userId, purchaseId, orderId, orderName, totalAmount,
                eventTitle, paymentMethod, approvedAt);
    }
}
