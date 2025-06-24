package org.codenbug.securityaop.aop;

import lombok.Getter;

@Getter
public class UserId {

	private String value;

	protected UserId() {
	}

	public UserId(String value) {
		this.value = value;
	}
}
