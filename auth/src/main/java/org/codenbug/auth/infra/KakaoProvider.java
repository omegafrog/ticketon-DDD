package org.codenbug.auth.infra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;
import org.codenbug.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KakaoProvider implements SocialProvider {

	/**
	 * application.yml에서 주입받는 카카오 OAuth 관련 설정값들
	 */
	@Value("${sns.kakao.url}")
	private String KAKAO_SNS_BASE_URL;  // 카카오 인증 기본 URL

	@Value("${sns.kakao.client.id}")
	private String KAKAO_SNS_CLIENT_ID;  // 카카오 애플리케이션 클라이언트 ID

	@Value("${sns.kakao.callback.url}")
	private String KAKAO_SNS_CALLBACK_URL;  // 인증 후 콜백받을 URL

	@Value("${sns.kakao.client.secret}")
	private String KAKAO_SNS_CLIENT_SECRET;  // 카카오 애플리케이션 시크릿 키

	@Value("${sns.kakao.token.url}")
	private String KAKAO_SNS_TOKEN_BASE_URL;  // 토큰 요청을 위한 URL

	@Value("#{'${allowed.redirect.domains}'.split(',')}")
	private List<String> allowedDomains;

	private final ObjectMapper objectMapper;

	public KakaoProvider(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String requestAccessToken(String code) {
		// RestTemplate 인스턴스 생성 (HTTP 요청을 위한 스프링 유틸리티)
		RestTemplate restTemplate = new RestTemplate();

		// HTTP 헤더 설정
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);  // Content-Type 설정

		// 토큰 요청에 필요한 파라미터 설정
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("code", code);  // 인증 코드
		params.add("client_id", KAKAO_SNS_CLIENT_ID);  // 클라이언트 ID
		params.add("client_secret", KAKAO_SNS_CLIENT_SECRET);  // 클라이언트 시크릿
		params.add("redirect_uri", KAKAO_SNS_CALLBACK_URL);  // 리다이렉트 URI
		params.add("grant_type", "authorization_code");  // 인증 타입

		// HTTP 요청 엔티티 생성 (헤더와 바디 포함)
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

		log.info("카카오 액세스 토큰 요청 시작 - redirect_uri: {}, client_id: {}",
			KAKAO_SNS_CALLBACK_URL, KAKAO_SNS_CLIENT_ID);

		try {
			// POST 요청 실행 및 응답 수신
			ResponseEntity<String> responseEntity =
				restTemplate.postForEntity(KAKAO_SNS_TOKEN_BASE_URL, requestEntity, String.class);

			// 응답 상태 확인 및 결과 반환
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
				log.info("카카오 액세스 토큰 요청 성공");
				return responseEntity.getBody();  // 성공 시 응답 바디 반환
			} else {
				log.error("카카오 액세스 토큰 요청 실패 - 응답 코드: {}, 응답 본문: {}",
					responseEntity.getStatusCode(), responseEntity.getBody());
				return createErrorResponse("카카오 로그인 요청 처리 실패 - 응답 코드: " + responseEntity.getStatusCode(),
					responseEntity.getBody());
			}
		} catch (Exception e) {
			log.error("카카오 액세스 토큰 요청 중 예외 발생: {}", e.getMessage(), e);
			return createErrorResponse("카카오 로그인 요청 처리 중 예외 발생", e.getMessage());
		}
	}

	@Override
	public String getUserInfo(SocialLoginType socialLoginType, String accessToken) {
		try {
			String url = "https://kapi.kakao.com/v2/user/me";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			con.setRequestMethod("GET");
			// Kakao의 경우 Authorization 헤더에 "Bearer" 토큰을 설정해야 함.
			con.setRequestProperty("Authorization", "Bearer " + accessToken);

			int responseCode = con.getResponseCode();
			if (responseCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				return response.toString();
			} else {
				throw new RuntimeException("Kakao API에서 사용자 정보를 가져오는 데 실패했습니다. 응답 코드: " + responseCode);
			}
		} catch (IOException e) {
			throw new RuntimeException("Kakao API 호출 중 오류 발생", e);
		}
	}

	@Override
	public UserInfo parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
		String socialId = "";
		String name = "";
		String email = "";
		try {
			socialId = objectMapper.readTree(userInfo).get("id").asText();
			name = objectMapper.readTree(userInfo).get("properties").get("nickname").asText();
			email = objectMapper.readTree(userInfo).get("kakao_account").get("email").asText();
		}catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return new UserInfo(socialId, name, socialLoginType.getName(), email, Role.USER.toString());
	}

	/**
	 * 에러 응답을 생성하는 헬퍼 메서드
	 * @param errorMessage 에러 메시지
	 * @param additionalInfo 추가 정보
	 * @return 에러 응답 문자열
	 */
	private String createErrorResponse(String errorMessage, String additionalInfo) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", errorMessage);
			errorResponse.put("error_description", additionalInfo != null ? additionalInfo : "");

			return objectMapper.writeValueAsString(errorResponse);
		} catch (Exception e) {
			log.error("에러 응답 JSON 생성 실패: {}", e.getMessage(), e);
			// JSON 생성 실패 시 fallback
			return "{\"error\": \"JSON 생성 오류\", \"error_description\": \"에러 응답 생성 중 문제가 발생했습니다.\"}";
		}
	}
}
