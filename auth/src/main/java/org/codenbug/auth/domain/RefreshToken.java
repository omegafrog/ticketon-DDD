package org.codenbug.auth.domain;

import lombok.Getter;

@Getter
public class RefreshToken {
	private String value;

	protected RefreshToken() {}

	public  RefreshToken(String value) {
		this.value = value;
	}
}
