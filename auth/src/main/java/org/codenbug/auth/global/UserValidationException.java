package org.codenbug.auth.global;

import org.codenbug.common.RsData;
import org.springframework.http.HttpStatus;

public class UserValidationException extends RuntimeException {
	private final HttpStatus status;
	private final RsData<?> rsData;

	public UserValidationException(HttpStatus status, RsData<?> rsData) {
		this.status = status;
		this.rsData = rsData;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public RsData<?> getRsData() {
		return rsData;
	}
}
