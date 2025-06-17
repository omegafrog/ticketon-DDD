package org.codenbug.user.ui;

import lombok.Getter;

@Getter
public class RegisterRequest {
	private String email;

	private String password;

	private String name;

	private Integer age;

	private String sex;

	private String phoneNum;

	private String location;

	public RegisterRequest(String email, String password, String name, Integer age, String sex, String phoneNum,
		String location) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.age = age;
		this.sex = sex;
		this.phoneNum = phoneNum;
		this.location = location;
	}
}
