package org.codenbug.auth.global;

import java.util.Arrays;
import java.util.Locale;

import lombok.Getter;

@Getter
public enum SocialLoginType {
	KAKAO("kakao", "https://kauth.kakao.com/oauth/authorize"),
	GOOGLE("google","https://accounts.google.com/o/oauth2/v2/auth");
	private final String name;
	private final String url;

	SocialLoginType(String name, String url) {
		this.name = name;
		this.url = url;
	}

	public static SocialLoginType fromPathValue(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Social login type is required.");
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		return Arrays.stream(values())
			.filter(type -> type.name.equals(normalized))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unsupported social login type: " + value));
	}

	public String getUrl(String clientId, String redirectUrl){
		return url
			+ """
			?client_id=%s\
			&redirect_uri=%s\
			&response_type=code\
			&scope=profile_nickname,profile_image,account_email,gender,age_range"""
			.formatted(clientId, redirectUrl);
	}

}
