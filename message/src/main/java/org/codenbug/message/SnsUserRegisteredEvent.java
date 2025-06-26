package org.codenbug.message;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SnsUserRegisteredEvent {
	private String securityUserId;
	private String name;
	private String sex;
	private int age;



	public SnsUserRegisteredEvent(String securityUserId, String name, int age, String sex) {
		this.securityUserId = securityUserId;
		this.name = name;
		this.sex = sex;
		this.age = age;
	}
}
