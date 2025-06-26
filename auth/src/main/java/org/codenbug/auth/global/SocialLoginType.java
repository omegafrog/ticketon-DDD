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

}
