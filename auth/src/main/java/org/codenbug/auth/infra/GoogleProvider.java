package org.codenbug.auth.infra;

import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;
import org.springframework.stereotype.Component;

@Component("GOOGLE")
public class GoogleProvider implements SocialProvider {
	@Override
	public String getOauthLoginUri() {
		return "";
	}

	@Override
	public String requestAccessToken(String code) {
		return "";
	}

	@Override
	public String getUserInfo(SocialLoginType socialLoginType, String accessToken) {
		return "";
	}

	@Override
	public UserInfo parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
		return null;
	}
}
