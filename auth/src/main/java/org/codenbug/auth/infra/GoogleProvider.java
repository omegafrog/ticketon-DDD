package org.codenbug.auth.infra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codenbug.auth.domain.Provider;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;
import org.codenbug.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component("GOOGLE")
@Slf4j
public class GoogleProvider implements SocialProvider {

	private final ObjectMapper objectMapper;
	@Value("${sns.google.url}")
	private String GOOGLE_SNS_BASE_URL;
	@Value("${sns.google.client.id}")
	private String GOOGLE_SNS_CLIENT_ID;
	@Value("${sns.google.callback.url}")
	private String GOOGLE_SNS_CALLBACK_URL;
	@Value("${sns.google.client.secret}")
	private String GOOGLE_SNS_CLIENT_SECRET;
	@Value("${sns.google.token.url}")
	private String GOOGLE_SNS_TOKEN_BASE_URL;

	public GoogleProvider(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String getOauthLoginUri() {
		Map<String, Object> params = new HashMap<>();
		params.put("scope",
			"https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email");
		params.put("response_type", "code");
		params.put("client_id", GOOGLE_SNS_CLIENT_ID);
		params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);

		String parameterString = params.entrySet().stream()
			.map(x -> x.getKey() + "=" + x.getValue())
			.collect(Collectors.joining("&"));

		return GOOGLE_SNS_BASE_URL + "?" + parameterString;
	}

	@Override
	public String requestAccessToken(String code) {
		RestTemplate restTemplate = new RestTemplate();

		Map<String, Object> params = new HashMap<>();
		params.put("code", code);
		params.put("client_id", GOOGLE_SNS_CLIENT_ID);
		params.put("client_secret", GOOGLE_SNS_CLIENT_SECRET);
		params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);
		params.put("grant_type", "authorization_code");

		ResponseEntity<String> responseEntity =
			restTemplate.postForEntity(GOOGLE_SNS_TOKEN_BASE_URL, params, String.class);

		if (responseEntity.getStatusCode() == HttpStatus.OK) {
			return responseEntity.getBody();
		}
		return "구글 로그인 요청 처리 실패";
	}

	@Override
	public String getUserInfo(SocialLoginType socialLoginType, String accessToken) {
		try {
			// accessToken을 URL 인코딩
			String encodedAccessToken = URLEncoder.encode(accessToken, "UTF-8");
			log.debug("Encoded access token: {}", encodedAccessToken);

			String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + encodedAccessToken;
			log.debug("Google API URL: {}", url);

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");

			int responseCode = con.getResponseCode();
			log.info("Google API response code: {}", responseCode);

			if (responseCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				log.info("Successfully received response from Google API.");
				return response.toString();
			} else {
				// 실패 시 에러 메시지와 상태 코드 출력
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				String inputLine;
				StringBuffer errorResponse = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					errorResponse.append(inputLine);
				}
				in.close();
				log.error("Google API call failed with response code: {}, error: {}", responseCode,
					errorResponse.toString());
				throw new RuntimeException("Google API에서 사용자 정보를 가져오는 데 실패했습니다. 응답 코드: " + responseCode + ", 에러 메시지: "
					+ errorResponse.toString());
			}
		} catch (IOException e) {
			log.error("Google API 호출 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("Google API 호출 중 오류 발생", e);
		}
	}

	@Override
	public UserInfo parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
		log.info(userInfo);
		try{
		JsonNode jsonNode = objectMapper.readTree(userInfo);
		// socialId와 name을 소셜 로그인 타입별로 분리
		String socialId = "";
		String name = "";
		String email = "";
		int age = 0;
		String sex = "";

		socialId = jsonNode.get("sub").asText(); // Google은 "sub"를 ID로 사용
		name = jsonNode.get("name").asText();    // Google에서 제공하는 이름
		if (jsonNode.has("email")) {
			email = jsonNode.get("email").asText(); // Google에서 제공하는 이메일
		}

		// SnsUser 객체에 정보 세팅
		return new UserInfo(socialId, name, socialLoginType.getName(), email, Role.USER.name(), age, "ETC");
		}catch (JsonProcessingException e){
			throw new RuntimeException(e);
		}
	}
}
