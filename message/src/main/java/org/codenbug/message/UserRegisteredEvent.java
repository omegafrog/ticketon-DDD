package org.codenbug.message;


import lombok.Getter;

@Getter
public class UserRegisteredEvent {
	public static final String TOPIC = "user-registered";
	private String securityUserId;
	private String userId;

	protected UserRegisteredEvent(){}

	public UserRegisteredEvent(String securityUserId, String userId) {
		this.securityUserId = securityUserId;
		this.userId = userId;
	}
}
