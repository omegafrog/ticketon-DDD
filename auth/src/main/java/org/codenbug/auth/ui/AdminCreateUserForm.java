package org.codenbug.auth.ui;

import org.codenbug.common.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserForm {
	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String password;

	@NotNull
	private Role role = Role.USER;
}
