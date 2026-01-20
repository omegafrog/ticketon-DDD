package org.codenbug.common.exception;

public class AccessDeniedException extends AuthException {
	public AccessDeniedException(String message) {
		super("401", message);
	}
}
