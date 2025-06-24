package org.codenbug.user.global.dto;

import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserInfo {
	private UserId userId;
	private String name;
	private Sex sex;
	private String phoneNum;
	private String location;
	private Integer age;

	public UserInfo(User user) {
		this.userId = user.getUserId();
		this.name = user.getName();
		this.sex = user.getSex();
		this.phoneNum = user.getPhoneNum();
		this.location = user.getLocation();
		this.age = user.getAge();
	}

}
