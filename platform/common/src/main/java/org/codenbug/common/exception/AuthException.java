package org.codenbug.common.exception;

public abstract class AuthException extends BaseException {
	public AuthException(String message) {
		super(message);
	}

	public AuthException(String code, String message) {
		super(code, message);
	}

	public AuthException(String code, String message, Throwable e) {
		super(code, message, e);
	}
}
