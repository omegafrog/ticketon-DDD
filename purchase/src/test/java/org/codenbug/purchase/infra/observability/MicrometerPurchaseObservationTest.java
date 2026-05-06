package org.codenbug.purchase.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.infra.JpaRefundRepository;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MicrometerPurchaseObservationTest {

	@Test
	void recordsPaymentReservationAndRefundMetrics() {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
		JpaRefundRepository refundRepository = mock(JpaRefundRepository.class);
		when(purchaseRepository.countByPaymentStatus(PaymentStatus.IN_PROGRESS)).thenReturn(11L);
		when(refundRepository.countByStatus(RefundStatus.COMPLETED)).thenReturn(4L);
		MicrometerPurchaseObservation observation = new MicrometerPurchaseObservation(meterRegistry,
			purchaseRepository, refundRepository);

		observation.recordReservationExpired(2);
		observation.recordRefundResult(RefundStatus.COMPLETED);

		assertThat(meterRegistry.counter("ticketon.reservation.expired").count()).isEqualTo(2.0);
		assertThat(meterRegistry.counter("ticketon.refund.result", "status", "COMPLETED").count())
			.isEqualTo(1.0);
		assertThat(meterRegistry.get("ticketon.payment.status.count").tag("status", "IN_PROGRESS")
			.gauge().value()).isEqualTo(11.0);
		assertThat(meterRegistry.get("ticketon.refund.status.count").tag("status", "COMPLETED")
			.gauge().value()).isEqualTo(4.0);
	}
}
