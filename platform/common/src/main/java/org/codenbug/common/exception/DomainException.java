package org.codenbug.common.exception;

public class DomainException extends BaseException {

	public DomainException(String message) {
		super("400", message);
	}

	public DomainException(String code, String message) {
		super(code, message);
	}

	public DomainException(String code, String message, Throwable e) {
		super(code, message, e);
	}
}
