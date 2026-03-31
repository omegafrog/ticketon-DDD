package org.codenbug.user.ui;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterValidationRequest {
	@NotBlank
	private String name;

	@NotNull
	@Min(0)
	@Max(150)
	private Integer age;

	@NotBlank
	private String sex;

	@NotBlank
	private String phoneNum;

	@NotBlank
	private String location;
}
