package org.codenbug.auth.global;

import org.codenbug.common.RsData;
import org.codenbug.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class UserValidationException extends DomainException {
	private final HttpStatus status;
	private final RsData<?> rsData;

	public UserValidationException(HttpStatus status, RsData<?> rsData) {
		super(String.valueOf(status.value()), rsData.getMessage());
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
