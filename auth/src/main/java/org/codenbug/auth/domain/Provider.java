package org.codenbug.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Provider {
	@Column(name = "provider", nullable = true)
	private String value;

	protected Provider() {
	}

	public Provider(String value) {
		this.value = value;
	}
}
