package org.codenbug.purchase.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PurchaseTest {

	@Test
	@DisplayName("결제 준비 시 결제 제한시간은 1시간이다")
	void 결제_준비_시_결제_제한시간은_한시간이다() {
		LocalDateTime before = LocalDateTime.now().plusHours(1).minusSeconds(1);
		Purchase purchase = new Purchase("event-1", "order-1", 1000, 1L, new UserId("user-1"));
		LocalDateTime after = LocalDateTime.now().plusHours(1).plusSeconds(1);

		assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
		assertThat(purchase.getPaymentDeadlineAt()).isBetween(before, after);
	}

	@Test
	@DisplayName("결제 제한시간 초과 시 예매는 만료된다")
	void 결제_제한시간이_초과되면_예매는_만료된다() {
		Purchase purchase = new Purchase("event-1", "order-1", 1000, 1L, new UserId("user-1"));
		ReflectionTestUtils.setField(purchase, "paymentDeadlineAt", LocalDateTime.now().minusSeconds(1));

		purchase.expireIfOverdue(LocalDateTime.now());

		assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
	}

	@Test
	@DisplayName("만료된 예매는 결제 확정할 수 없다")
	void 만료된_예매는_결제_확정할_수_없다() {
		Purchase purchase = new Purchase("event-1", "order-1", 1000, 1L, new UserId("user-1"));
		ReflectionTestUtils.setField(purchase, "paymentDeadlineAt", LocalDateTime.now().minusSeconds(1));

		assertThatThrownBy(() -> purchase.ensureConfirmableAt(LocalDateTime.now()))
			.isInstanceOf(IllegalStateException.class);
		assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
	}

	@Test
	@DisplayName("사용자는 결제 대기 예매를 포기할 수 있다")
	void 사용자는_결제_대기_예매를_포기할_수_있다() {
		Purchase purchase = new Purchase("event-1", "order-1", 1000, 1L, new UserId("user-1"));

		purchase.cancelPending();

		assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	@DisplayName("완료된 구매는 다시 결제 완료로 확정할 수 없다")
	void 완료된_구매는_다시_결제완료로_확정할_수_없다() {
		Purchase purchase = new Purchase("event-1", "order-1", 1000, 1L, new UserId("user-1"));
		purchase.markAsCompleted();

		assertThatThrownBy(purchase::markAsCompleted)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("결제 대기 상태가 아닙니다.");
	}
}
