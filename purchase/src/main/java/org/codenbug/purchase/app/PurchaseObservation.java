package org.codenbug.purchase.app;

import org.codenbug.purchase.domain.RefundStatus;

public interface PurchaseObservation {
	void recordReservationExpired(int count);

	void recordRefundResult(RefundStatus status);

	static PurchaseObservation noop() {
		return NoopPurchaseObservation.INSTANCE;
	}
}
