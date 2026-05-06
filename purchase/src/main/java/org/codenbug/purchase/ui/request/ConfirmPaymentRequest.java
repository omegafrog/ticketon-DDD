package org.codenbug.purchase.ui.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {
	@NotBlank
	private String purchaseId;

	@NotBlank
	private String paymentKey;

	@NotBlank
	private String orderId;

	@NotNull
	@Min(1)
	private Integer amount;

	@NotBlank
	private String provider;
}
