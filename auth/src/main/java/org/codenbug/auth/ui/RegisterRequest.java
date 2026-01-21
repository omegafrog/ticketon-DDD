package org.codenbug.auth.ui;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RegisterRequest {
	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String password;

	@NotBlank
	private String name;

	@NotNull
	@Min(0)
	private Integer age;

	@NotBlank
	private String sex;

	@NotBlank
	private String phoneNum;

	@NotBlank
	private String location;
}
