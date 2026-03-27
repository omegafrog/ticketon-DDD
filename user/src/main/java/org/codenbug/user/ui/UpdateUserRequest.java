package org.codenbug.user.ui;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateUserRequest {

	@Size(min = 1, max = 50)
	private String name;

	@Min(0)
	@Max(150)
	private Integer age;

	@Size(min = 1, max = 100)
	private String location;

	@Size(min = 1, max = 20)
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
