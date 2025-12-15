package org.codenbug.common.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
	private String code;
	private String message;

	public BaseException(String message) {
		super(message);
	}

	public BaseException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public BaseException(String code, String message, Throwable e) {
		super(message, e);
		this.code = code;
		this.message = message;
	}

}
