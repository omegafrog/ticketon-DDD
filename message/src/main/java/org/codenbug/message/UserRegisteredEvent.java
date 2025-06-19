package org.codenbug.message;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserRegisteredEvent {
	private String userId;
	private String email;
	private String password;
	private String role;
	private LocalDateTime createdAt;

	public UserRegisteredEvent(String userId, String email, String password, String role) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.role = role;
		createdAt = LocalDateTime.now();
	}
}
