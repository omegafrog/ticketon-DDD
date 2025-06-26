package org.codenbug.auth.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class RegisterRequest {
	private String email;
	private String password;
	private String name;
	private Integer age;
	private String sex;
	private String phoneNum;
	private String location;
}
