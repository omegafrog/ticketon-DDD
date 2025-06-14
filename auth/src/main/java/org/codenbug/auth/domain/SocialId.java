package org.codenbug.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class SocialId {

	@Column(name = "social_id", nullable = true, unique = true)
	private String value;

	protected SocialId() {
	}

	public SocialId(String value) {
		this.value = value;
	}
}
