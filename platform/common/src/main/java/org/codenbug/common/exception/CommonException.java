package org.codenbug.common.exception;

public class CommonException extends BaseException {

	public CommonException(String message) {
		super("400", message);
	}

	public CommonException(String code, String message) {
		super(code, message);
	}

	public CommonException(String code, String message, Throwable e) {
		super(code, message, e);
	}
}
