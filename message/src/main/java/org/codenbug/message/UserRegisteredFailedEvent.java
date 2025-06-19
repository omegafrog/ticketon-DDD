package org.codenbug.message;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserRegisteredFailedEvent {
	private String userId;
	private LocalDateTime createdAt;

	public UserRegisteredFailedEvent(String userId) {
		this.userId = userId;
		this.createdAt = LocalDateTime.now();
	}
}
