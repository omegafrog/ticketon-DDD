package org.codenbug.user.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterValidationRequest {
	private String name;
	private Integer age;
	private String sex;
	private String phoneNum;
	private String location;
}
