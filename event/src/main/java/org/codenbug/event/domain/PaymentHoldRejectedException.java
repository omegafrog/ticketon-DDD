package org.codenbug.event.domain;

public class PaymentHoldRejectedException extends RuntimeException {
	public PaymentHoldRejectedException(String message) {
		super(message);
	}
}
