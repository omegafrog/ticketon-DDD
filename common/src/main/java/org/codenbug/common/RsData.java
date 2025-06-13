package org.codenbug.common;

import lombok.Getter;

@Getter
public class RsData<T> {
	private T data;
	private String code;
	private String message;

	protected RsData(){}
	public RsData( String code, String message, T data) {
		this.data = data;
		this.code = code;
		this.message = message;
	}
}
