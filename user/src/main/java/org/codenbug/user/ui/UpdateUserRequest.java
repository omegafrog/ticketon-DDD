package org.codenbug.user.ui;

import lombok.Getter;

@Getter
public class UpdateUserRequest {

	private String name;
	private Integer age;
	private String location;
	private String phoneNum;

	protected UpdateUserRequest() {
	}

	public UpdateUserRequest(String name, Integer age, String location, String phoneNum) {
		this.name = name;
		this.age = age;
		this.location = location;
		this.phoneNum = phoneNum;
	}
}