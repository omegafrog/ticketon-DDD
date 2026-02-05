package org.codenbug.purchase.app;

public enum PaymentProvider {
	TOSS;

	public static PaymentProvider from(String raw) {
		if (raw == null || raw.isBlank()) {
			return TOSS;
		}
		return PaymentProvider.valueOf(raw.trim().toUpperCase());
	}
}
