package org.codenbug.purchase.global;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {
	@NotBlank
	private String eventId;

	@NotBlank
	private String orderId;

	@NotNull
	@Min(1)
	private Integer amount;
}
