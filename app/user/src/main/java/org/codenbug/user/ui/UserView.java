package org.codenbug.user.ui;

import java.time.LocalDateTime;

import org.codenbug.user.domain.Sex;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserView {
	private String userId;
	private String name;
	private Sex sex;
	private String phoneNum;
	private String location;
	private Integer age;
	private String role;
	private String email;
	private LocalDateTime createdAt;
}
