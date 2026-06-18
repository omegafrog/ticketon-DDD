package org.codenbug.auth.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class KakaoProviderTest {

	@Test
	@DisplayName("카카오 사용자 정보에 선택 동의 항목이 없으면 기본값으로 파싱한다")
	void parseUserInfoUsesDefaultsWhenOptionalKakaoFieldsAreMissing() {
		KakaoProvider provider = new KakaoProvider(new ObjectMapper());
		String userInfo = """
			{
			  "id": 12345,
			  "properties": {
			    "nickname": "Kakao User"
			  },
			  "kakao_account": {
			    "email": "kakao@example.com"
			  }
			}
			""";

		UserInfo result = provider.parseUserInfo(userInfo, SocialLoginType.KAKAO);

		assertThat(result.getSocialId().getValue()).isEqualTo("12345");
		assertThat(result.getName()).isEqualTo("Kakao User");
		assertThat(result.getEmail()).isEqualTo("kakao@example.com");
		assertThat(result.getAge()).isZero();
		assertThat(result.getSex()).isEqualTo("ETC");
	}

	@Test
	@DisplayName("카카오 성별과 연령대가 있으면 서비스 값으로 변환한다")
	void parseUserInfoMapsKakaoGenderAndAgeRange() {
		KakaoProvider provider = new KakaoProvider(new ObjectMapper());
		String userInfo = """
			{
			  "id": 12345,
			  "properties": {
			    "nickname": "Kakao User"
			  },
			  "kakao_account": {
			    "email": "kakao@example.com",
			    "age_range": "20~29",
			    "gender": "female"
			  }
			}
			""";

		UserInfo result = provider.parseUserInfo(userInfo, SocialLoginType.KAKAO);

		assertThat(result.getAge()).isEqualTo(20);
		assertThat(result.getSex()).isEqualTo("FEMALE");
	}
}
