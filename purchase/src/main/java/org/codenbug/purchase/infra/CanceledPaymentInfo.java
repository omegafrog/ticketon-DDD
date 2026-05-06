package org.codenbug.purchase.infra;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.codenbug.purchase.domain.PaymentCancellationInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CanceledPaymentInfo {
	private String paymentKey;
	private String orderId;
	private String status;
	private String method;
	private Integer totalAmount;
	private Receipt receipt;
	private List<CancelDetail> cancels;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Receipt {
		private String url;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CancelDetail {
		private Integer cancelAmount;
		private String canceledAt;
		private String cancelReason;
	}

	public PaymentCancellationInfo toDomain() {
		List<PaymentCancellationInfo.CancelDetail> domainCancels = cancels == null ? List.of() : cancels.stream()
			.map(cancel -> new PaymentCancellationInfo.CancelDetail(cancel.getCancelAmount(), cancel.getCanceledAt(),
				cancel.getCancelReason()))
			.toList();
		return new PaymentCancellationInfo(paymentKey, orderId, status, method, totalAmount,
			receipt == null ? null : receipt.getUrl(), domainCancels);
	}
}
