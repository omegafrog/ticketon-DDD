package org.codenbug.common.exception;

public abstract class JwtException extends BaseException {
	public JwtException(String message) {
		super(message);
	}

	public JwtException(String code, String message) {
		super(code, message);
	}

	public JwtException(String code, String message, Throwable e) {
		super(code, message, e);
	}
}
