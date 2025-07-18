package org.codenbug.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class SecurityUserId {
	@Column(name = "security_user_id")
	private String value;

	protected SecurityUserId() {
	}

	public SecurityUserId(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
