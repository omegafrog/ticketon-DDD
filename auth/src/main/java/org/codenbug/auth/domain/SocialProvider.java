package org.codenbug.auth.domain;

import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;

public interface SocialProvider {
	String requestAccessToken(String code);

	String getUserInfo(SocialLoginType socialLoginType, String accessToken);

	UserInfo parseUserInfo(String userInfo, SocialLoginType socialLoginType);
}
