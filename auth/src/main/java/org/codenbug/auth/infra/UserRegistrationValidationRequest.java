package org.codenbug.auth.infra;

import lombok.Getter;

@Getter
public class UserRegistrationValidationRequest {
	private final String name;
	private final Integer age;
	private final String sex;
	private final String phoneNum;
	private final String location;

	public UserRegistrationValidationRequest(String name, Integer age, String sex, String phoneNum,
			String location) {
		this.name = name;
		this.age = age;
		this.sex = sex;
		this.phoneNum = phoneNum;
		this.location = location;
	}
}
