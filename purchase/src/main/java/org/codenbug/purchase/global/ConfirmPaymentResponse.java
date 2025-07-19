package org.codenbug.purchase.global;

import java.time.LocalDateTime;

import org.codenbug.purchase.domain.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentResponse {
	private String paymentKey;
	private String orderId;
	private String orderName;
	private int totalAmount;
	private String status;
	private PaymentMethod method;
	private LocalDateTime approvedAt;
	private Receipt receipt;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Receipt {
		private String url;
	}
}
