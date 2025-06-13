package org.codenbug.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;

@Embeddable
@Getter
public class SocialInfo {
	@Embedded
	private SocialId socialId;

	@Embedded
	private Provider provider; // 소셜 로그인 제공자 (Google, Kakao, Naver 등)

	private boolean isSocialUser;

	protected SocialInfo() {
	}

	public SocialInfo(SocialId socialId, Provider provider, boolean isSocialUser) {
		this.socialId = socialId;
		this.provider = provider;
		this.isSocialUser = isSocialUser;
		validate();
	}

	protected void validate() {
		if (isSocialUser) {
			if (this.socialId == null || this.provider == null)
				throw new IllegalArgumentException("Social User's socialId or provider must not be null");
		}
	}
}
