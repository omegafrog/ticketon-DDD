package org.codenbug.common.exception;

public class BlacklistedRefreshTokenException extends JwtException {
	public BlacklistedRefreshTokenException(String message) {
		super("401", message);
	}
}
