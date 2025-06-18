package org.codenbug.user.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class UserId {

	@Column(name = "user_id")
	private String value;

	protected UserId(){}
	public UserId(String value){
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UserId userId))
			return false;
		return Objects.equals(value, userId.value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}
}
