package org.codenbug.auth.domain;

import lombok.Getter;

@Getter
public class AccessToken {
	private String value;
	private String type;

	protected AccessToken() {}

	public AccessToken(String value, String type) {
		this.value = value;
		this.type = type;
	}
}
