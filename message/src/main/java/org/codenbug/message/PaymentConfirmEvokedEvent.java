package org.codenbug.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Event that is published when a payment is completed
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmEvokedEvent {
    public static final String PAYMENT_CONFIRM_INVOKED_TOPIC = "payment-confirm-evoked";
    private String pid;
    private String orderId;
    private Integer amount;
    private String userId;
}
