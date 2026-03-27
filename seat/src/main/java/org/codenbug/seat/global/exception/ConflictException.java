package org.codenbug.seat.global.exception;

import org.codenbug.common.exception.DomainException;

public class ConflictException extends DomainException {
	public ConflictException(String s) {
		super("409", s);
	}
}
