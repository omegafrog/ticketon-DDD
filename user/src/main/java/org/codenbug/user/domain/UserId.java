package org.codenbug.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserId {

	@Column(name = "user_id")
	private String value;

	protected UserId(){}
	public UserId(String value){
		this.value = value;
	}
}
