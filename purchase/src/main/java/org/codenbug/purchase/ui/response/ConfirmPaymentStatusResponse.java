package org.codenbug.purchase.ui.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfirmPaymentStatusResponse {
	private String purchaseId;
	private String status;
	private String paymentStatus;
	private String message;
	private LocalDateTime updatedAt;
}
