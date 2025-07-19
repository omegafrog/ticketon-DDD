package org.codenbug.purchase.global;

import org.codenbug.purchase.domain.PurchaseId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentResponse {
	private PurchaseId purchaseId;
	private String status;
}
