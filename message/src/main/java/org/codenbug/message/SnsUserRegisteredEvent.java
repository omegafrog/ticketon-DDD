package org.codenbug.message;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SnsUserRegisteredEvent {
	private String securityUserId;
	private String name;

	public SnsUserRegisteredEvent(String securityUserId, String name){
		this.securityUserId = securityUserId;
		this.name = name;
	}
}
