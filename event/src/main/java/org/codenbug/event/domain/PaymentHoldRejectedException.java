package org.codenbug.event.domain;

import org.codenbug.common.exception.DomainException;

public class PaymentHoldRejectedException extends DomainException {
	public PaymentHoldRejectedException(String message) {
		super("409", message);
	}
}
