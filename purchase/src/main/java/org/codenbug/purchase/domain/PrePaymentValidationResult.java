package org.codenbug.purchase.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrePaymentValidationResult {
    private final String eventId;
    private final Long eventVersion;
    private final String eventStatus;
    private final PurchaseId purchaseId;
    private final String userId;
    private final Integer amount;
    private final String orderId;
    
    public boolean isEventStatusValid() {
        return "OPEN".equals(eventStatus);
    }
}