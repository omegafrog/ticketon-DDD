package org.codenbug.user.global.dto;

import java.time.LocalDateTime;

import org.codenbug.common.Role;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserInfo {
	private String userId;
	private String name;
	private Sex sex;
	private String phoneNum;
	private String location;
	private Integer age;
	private String role;
	private String email;
	private LocalDateTime createdAt;

	public UserInfo(User user, String email, Role role) {
		this.userId = user.getUserId().getValue();
		this.name = user.getName();
		this.sex = user.getSex();
		this.phoneNum = user.getPhoneNum();
		this.location = user.getLocation();
		this.age = user.getAge();
		this.role = role.name();
		this.email = email;
		this.createdAt = user.getCreatedAt();
	}

}
