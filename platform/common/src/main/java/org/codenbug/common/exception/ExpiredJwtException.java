package org.codenbug.common.exception;

public class ExpiredJwtException extends JwtException {
	public ExpiredJwtException(String code, String message) {
		super(code, message);
	}

	public ExpiredJwtException(String code, String message, Throwable e) {
		super(code, message, e);
	}

	public ExpiredJwtException(String message) {
		super(message);
	}
}
