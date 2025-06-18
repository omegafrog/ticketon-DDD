package org.codenbug.user.domain;

import com.fasterxml.uuid.Generators;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "members")
@Getter
public class User {

	@EmbeddedId
	private UserId userId;

	@Column(name = "name", length = 255)
	private String name;

	@Column(name = "sex", nullable = false)
	@Enumerated(EnumType.STRING)
	private Sex sex;

	@Column(name = "phoneNum", length = 255)
	private String phoneNum;

	@Column(name = "addresses", length = 255)
	private String location;

	@Column(name = "age", nullable = false)
	private Integer age;

	protected User() {
	}

	public User(String name, Sex sex, String phoneNum, String location, Integer age) {
		this.userId = generateUserId();
		this.name = name;
		this.sex = sex;
		this.phoneNum = phoneNum;
		this.location = location;
		this.age = age;
	}

	private UserId generateUserId() {
		return new UserId(Generators.timeBasedEpochGenerator().generate().toString());
	}
}
