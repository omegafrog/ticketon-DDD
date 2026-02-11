package org.codenbug.user.ui;

import org.codenbug.user.domain.SecurityUserId;

import lombok.Getter;

@Getter
public class RegisterRequest {
	private SecurityUserId securityUserId;
	private String name;

	private Integer age;

	private String sex;

	private String phoneNum;

	private String location;

	public RegisterRequest( SecurityUserId securityUserId, String name, Integer age, String sex, String phoneNum,
		String location) {
		this.securityUserId = securityUserId;
		this.name = name;
		this.age = age;
		this.sex = sex;
		this.phoneNum = phoneNum;
		this.location = location;
	}
}
