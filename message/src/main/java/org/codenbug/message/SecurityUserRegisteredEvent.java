package org.codenbug.message;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecurityUserRegisteredEvent {


	private String securityUserId;
	private String name;
	private Integer age;
	private String sex;
	private String phoneNum;
	private String location;
	private LocalDateTime createdAt;

	public SecurityUserRegisteredEvent(String securityUserId, String name, Integer age,
		String sex, String phoneNum, String location) {
		this.securityUserId = securityUserId;
		this.name = name;
		this.age = age;
		this.sex = sex;
		this.phoneNum = phoneNum;
		this.location = location;

		createdAt = LocalDateTime.now();
	}
}
