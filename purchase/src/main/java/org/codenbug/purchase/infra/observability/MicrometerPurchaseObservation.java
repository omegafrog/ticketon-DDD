package org.codenbug.purchase.infra.observability;

import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.app.PurchaseObservation;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.infra.JpaRefundRepository;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MicrometerPurchaseObservation implements PurchaseObservation {
	private final MeterRegistry meterRegistry;

	public MicrometerPurchaseObservation(MeterRegistry meterRegistry, PurchaseRepository purchaseRepository,
		JpaRefundRepository refundRepository) {
		this.meterRegistry = meterRegistry;
		for (PaymentStatus status : PaymentStatus.values()) {
			Gauge.builder("ticketon.payment.status.count", purchaseRepository,
					repository -> repository.countByPaymentStatus(status))
				.description("Payment count by processing status")
				.tags("status", status.name())
				.register(meterRegistry);
		}
		for (RefundStatus status : RefundStatus.values()) {
			Gauge.builder("ticketon.refund.status.count", refundRepository,
					repository -> repository.countByStatus(status))
				.description("Refund count by status")
				.tags("status", status.name())
				.register(meterRegistry);
		}
	}

	@Override
	public void recordReservationExpired(int count) {
		if (count <= 0) {
			return;
		}
		Counter.builder("ticketon.reservation.expired")
			.description("Expired reservation count")
			.register(meterRegistry)
			.increment(count);
	}

	@Override
	public void recordRefundResult(RefundStatus status) {
		Counter.builder("ticketon.refund.result")
			.description("Refund result count")
			.tags("status", status.name())
			.register(meterRegistry)
			.increment();
	}
}
