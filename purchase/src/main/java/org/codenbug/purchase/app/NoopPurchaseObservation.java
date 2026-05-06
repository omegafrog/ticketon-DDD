package org.codenbug.purchase.app;

import org.codenbug.purchase.domain.RefundStatus;

enum NoopPurchaseObservation implements PurchaseObservation {
	INSTANCE;

	@Override
	public void recordReservationExpired(int count) {
	}

	@Override
	public void recordRefundResult(RefundStatus status) {
	}
}
