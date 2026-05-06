package org.codenbug.purchase.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundDomainServiceTest {

	private final RefundDomainService refundDomainService = new RefundDomainService();

	@Test
	@DisplayName("부분 환불은 허용하지 않는다")
	void 부분_환불_거부() {
		Purchase purchase = completedPurchase();

		assertThatThrownBy(() -> refundDomainService.processUserRefund(purchase, 500, "cancel",
			new UserId("user-1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("부분 환불");
	}

	@Test
	@DisplayName("전체 환불은 구매 상태를 환불 완료로 전이한다")
	void 전체_환불만_허용() {
		Purchase purchase = completedPurchase();

		RefundDomainService.RefundResult result = refundDomainService.processUserRefund(purchase, 1000,
			"cancel", new UserId("user-1"));

		assertThat(result.isFullRefund()).isTrue();
		assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(result.getRefund().getRefundAmountValue()).isEqualTo("1000");
	}

	@Test
	@DisplayName("매니저 환불도 전체 환불만 허용한다")
	void 매니저_부분_환불_거부() {
		Purchase purchase = completedPurchase();

		assertThatThrownBy(() -> refundDomainService.processManagerRefund(purchase, 500,
			"weather", new UserId("manager-1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("부분 환불");
	}

	private Purchase completedPurchase() {
		Purchase purchase = new Purchase("event-1", "order-1", 1000, 1L, new UserId("user-1"));
		purchase.markAsCompleted();
		return purchase;
	}
}
