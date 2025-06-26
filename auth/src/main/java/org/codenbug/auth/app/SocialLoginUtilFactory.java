package org.codenbug.auth.app;

import org.codenbug.auth.infra.GoogleProvider;
import org.codenbug.auth.infra.KakaoProvider;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

public class SocialLoginUtilFactory {

	public static SocialProvider getProvider(SocialLoginType type, ObjectMapper objectMapper){
		switch (type) {
			case KAKAO:
				return new KakaoProvider(objectMapper);
			case GOOGLE:
				return new GoogleProvider();
			default:
				throw new IllegalArgumentException();
		}
	}
}
