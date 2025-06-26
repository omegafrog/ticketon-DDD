package org.codenbug.message;


import lombok.Getter;

@Getter
public class UserRegisteredEvent {
	private String securityUserId;
	private String userId;

	protected UserRegisteredEvent(){}

	public UserRegisteredEvent(String securityUserId, String userId) {
		this.securityUserId = securityUserId;
		this.userId = userId;
	}
}
