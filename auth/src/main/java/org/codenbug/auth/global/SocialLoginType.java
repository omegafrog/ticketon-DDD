package org.codenbug.auth.global;

import lombok.Getter;

@Getter
public enum SocialLoginType {
	KAKAO("kakao", "https://kauth.kakao.com/oauth/authorize"),
	GOOGLE("google","https://accounts.google.com/o/oauth2/v2/auth");
	String name;
	String url;

	SocialLoginType(String name, String url) {
		this.name = name;
		this.url = url;
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
