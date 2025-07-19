package org.codenbug.purchase.global;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.codenbug.purchase.infra.CanceledPaymentInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelPaymentResponse {
	private String paymentKey;
	private String orderId;
	private String status;
	private String method;
	private Integer totalAmount;
	private String receiptUrl;
	private List<CancelDetail> cancels;

	public static CancelPaymentResponse of(CanceledPaymentInfo canceledPaymentInfo){
		return CancelPaymentResponse.builder()
			.paymentKey(canceledPaymentInfo.getPaymentKey())
			.orderId(canceledPaymentInfo.getOrderId())
			.status(canceledPaymentInfo.getStatus())
			.method(canceledPaymentInfo.getMethod())
			.totalAmount(canceledPaymentInfo.getTotalAmount())
			.receiptUrl(canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
			.cancels(canceledPaymentInfo.getCancels().stream()
				.map(c -> CancelPaymentResponse.CancelDetail.builder()
					.cancelAmount(c.getCancelAmount())
					.canceledAt(OffsetDateTime.parse(c.getCanceledAt()).toLocalDateTime())
					.cancelReason(c.getCancelReason())
					.build())
				.toList())
			.build();
	}


	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CancelDetail {
		private Integer cancelAmount;
		private LocalDateTime canceledAt;
		private String cancelReason;
	}
}