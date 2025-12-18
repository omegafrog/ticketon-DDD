package org.codenbug.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class UserId {
	@Column(name = "user_id")
	private String value;

	protected UserId(){};
	public UserId(String value){
		this.value = value;
	}
}
