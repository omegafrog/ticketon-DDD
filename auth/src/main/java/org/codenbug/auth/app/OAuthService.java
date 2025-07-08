package org.codenbug.auth.app;

import java.util.Map;
import java.util.Optional;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.SocialInfo;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.message.SnsUserRegisteredEvent;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;
import org.codenbug.auth.ui.SocialLoginResponse;
import org.codenbug.common.Role;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OAuthService {

	private final SecurityUserRepository repository;
	private final ApplicationEventPublisher publisher;
	private final ObjectMapper objectMapper;
	private final ProviderFactory factory;
	@Value("${custom.jwt.secret}")
	private String jwtSecret;

	public OAuthService(SecurityUserRepository repository, ApplicationEventPublisher publisher,
		ObjectMapper objectMapper, ProviderFactory factory) {
		this.repository = repository;
		this.publisher = publisher;
		this.objectMapper = objectMapper;
		this.factory = factory;
	}

	@Transactional
	public SocialLoginResponse requestAccessTokenAndSaveUser(SocialLoginType socialLoginType, String code) {

		SocialProvider provider = factory.getProvider(socialLoginType.getName().toUpperCase());
		// 1. 액세스 토큰을 포함한 JSON 응답을 요청
		String accessTokenJson = provider.requestAccessToken(code);
		log.debug(">> 소셜 로그인 액세스 토큰 응답: {}", accessTokenJson);

		// 2. JSON에서 액세스 토큰만 추출
		String accessToken = extractAccessTokenFromJson(accessTokenJson);

		if (accessToken == null) {
			log.error(">> 액세스 토큰 추출 실패");
			throw new RuntimeException("access token 추출에 실패했습니다.");
		}
		log.debug(">> 추출된 액세스 토큰: {}", accessToken);

		// 3. 액세스 토큰을 사용해 사용자 정보 요청
		String userInfo = provider.getUserInfo(socialLoginType, accessToken);
		log.debug(">> 소셜 API에서 받은 사용자 정보: {}", userInfo);

		// 4. 사용자 정보를 파싱하여 SnsUser 객체 생성
		UserInfo parsed = provider.parseUserInfo(userInfo, socialLoginType);
		log.debug(">> 파싱된 사용자 정보: socialId={}, name={}, provider={}, email={}",
			parsed.getSocialId(), parsed.getName(), parsed.getProvider(), parsed.getEmail());

		// 5. 기존 사용자 확인 후 처리
		Optional<SecurityUser> existingUser = repository.findSecurityUserByEmail(parsed.getEmail());

		TokenInfo tokenInfo = null;
		// 이미 존재하는 사용자라면 토큰 생성
		if (existingUser.isPresent()) {
			tokenInfo = Util.generateTokens(
				Map.of("userId", existingUser.get().getUserId().toString(), "role", existingUser.get().getRole(),
					"email", existingUser.get().getEmail()), Util.Key.convertSecretKey(jwtSecret));
			return new SocialLoginResponse(tokenInfo);
		}

		// 새 사용자라면 저장
		log.debug(">> 신규 사용자 등록 이벤트 발행: socialId={}, provider={}", parsed.getSocialId(), parsed.getProvider());

		SecurityUser newUser = new SecurityUser(new SocialInfo(parsed.getSocialId(), parsed.getProvider(), true),
			parsed.getEmail(), null, Role.USER.name());
		repository.save(newUser);
		publisher.publishEvent(new SnsUserRegisteredEvent(newUser.getSecurityUserId().toString(), parsed.getName(),
			parsed.getAge(), parsed.getSex()));

		// 6. SNS 사용자용 JWT 토큰 생성
		// SNS 사용자용 토큰 생성 메서드 사용
		log.debug(">> SNS 사용자 토큰 생성: socialId={}, provider={}", parsed.getSocialId(), parsed.getProvider());

		tokenInfo = Util.generateTokens(
			Map.of("socialId", parsed.getSocialId().getValue(), "provider", parsed.getProvider().getValue()),
			Util.Key.convertSecretKey(jwtSecret));

		log.debug(">> SNS 사용자 토큰 생성 완료: accessToken={}, refreshToken={}",
			tokenInfo.getAccessToken(), tokenInfo.getRefreshToken());

		// 7. 토큰 및 사용자 정보를 포함한 응답 반환
		return new SocialLoginResponse(tokenInfo);
	}

	// JSON에서 액세스 토큰만 추출하는 메서드
	private String extractAccessTokenFromJson(String accessTokenJson) {
		// 응답이 null이거나 비어있는지 확인
		if (accessTokenJson == null || accessTokenJson.trim().isEmpty()) {
			log.error(">> 액세스 토큰 응답이 null이거나 비어있습니다.");
			return null;
		}

		// 응답 내용 로깅 (디버깅용)
		log.debug(">> 액세스 토큰 응답 내용: {}", accessTokenJson);

		// 응답이 JSON 형태인지 간단히 확인
		String trimmed = accessTokenJson.trim();
		if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
			log.error(">> 응답이 JSON 형태가 아닙니다. 응답 내용: {}", accessTokenJson);

			// HTML 에러 페이지인지 확인
			if (trimmed.toLowerCase().contains("<html") || trimmed.toLowerCase().contains("<!doctype")) {
				log.error(">> HTML 에러 페이지가 반환되었습니다. 카카오 OAuth 설정(redirect_uri, client_id 등)을 확인해주세요.");
				throw new RuntimeException(
					"카카오 OAuth 설정 오류: HTML 에러 페이지가 반환되었습니다. redirect_uri와 카카오 애플리케이션 설정을 확인해주세요.");
			}

			// 한글 에러 메시지인지 확인
			if (trimmed.contains("카카오") || trimmed.contains("오류") || trimmed.contains("실패")) {
				log.error(">> 카카오에서 한글 에러 메시지가 반환되었습니다: {}", trimmed);
				throw new RuntimeException("카카오 OAuth 인증 실패: " + trimmed);
			}

			throw new RuntimeException("유효하지 않은 응답 형식입니다: " + trimmed);
		}

		// JSON을 파싱하여 액세스 토큰만 추출
		try {
			JsonNode jsonNode = objectMapper.readTree(accessTokenJson);

			// access_token 필드 확인
			if (jsonNode.has("access_token")) {
				String accessToken = jsonNode.get("access_token").asText();
				log.debug(">> 액세스 토큰 추출 성공");
				return accessToken;
			} else {
				log.error(">> JSON 응답에 access_token 필드가 없습니다. 응답: {}", accessTokenJson);

				// 에러 정보가 있는지 확인
				if (jsonNode.has("error")) {
					String error = jsonNode.get("error").asText();
					String errorDescription = jsonNode.has("error_description") ?
						jsonNode.get("error_description").asText() : "설명 없음";
					String errorCode = jsonNode.has("error_code") ?
						jsonNode.get("error_code").asText() : "";

					log.error(">> OAuth 에러 - error: {}, description: {}, code: {}", error, errorDescription, errorCode);

					// 사용자에게 도움이 되는 구체적인 에러 메시지 생성
					String userFriendlyMessage = createUserFriendlyErrorMessage(error, errorDescription, errorCode);
					throw new RuntimeException(userFriendlyMessage);
				}

				return null;
			}
		} catch (JsonProcessingException e) {
			log.error(">> JSON 파싱 실패. 응답 내용: {}, 에러: {}", accessTokenJson, e.getMessage(), e);

			// JSON 파싱 에러의 경우 원본 응답을 분석해서 더 유용한 메시지 제공
			if (accessTokenJson.contains("error") && accessTokenJson.contains("invalid_grant")) {
				throw new RuntimeException("카카오 OAuth 인증 실패: authorization code가 유효하지 않거나 만료되었습니다. " +
					"카카오 개발자 콘솔에서 Redirect URI 설정을 확인하거나 새로운 authorization code로 다시 시도해주세요.");
			}

			throw new RuntimeException("액세스 토큰 응답 파싱 실패: " + e.getMessage(), e);
		}
	}

	private String createUserFriendlyErrorMessage(String error, String errorDescription, String errorCode) {
		StringBuilder message = new StringBuilder("카카오 로그인 실패: ");

		switch (error) {
			case "invalid_grant":
				if ("KOE320".equals(errorCode)) {
					message.append("인증 코드를 찾을 수 없습니다. ");
					message.append("다음 사항을 확인해주세요:\n");
					message.append("1. 카카오 개발자 콘솔에서 Redirect URI가 올바르게 설정되었는지 확인\n");
					message.append("2. authorization code가 이미 사용되었는지 확인 (한 번만 사용 가능)\n");
					message.append("3. authorization code가 만료되었는지 확인 (10분 유효)");
				} else {
					message.append("인증 정보가 유효하지 않습니다. ").append(errorDescription);
				}
				break;
			case "invalid_client":
				message.append("카카오 애플리케이션 설정 오류입니다. 클라이언트 ID 또는 Secret을 확인해주세요.");
				break;
			case "invalid_request":
				message.append("요청 형식이 올바르지 않습니다. ").append(errorDescription);
				break;
			case "unauthorized_client":
				message.append("허가되지 않은 클라이언트입니다. 카카오 개발자 콘솔에서 애플리케이션 상태를 확인해주세요.");
				break;
			default:
				message.append(error).append(" - ").append(errorDescription);
		}

		if (!errorCode.isEmpty()) {
			message.append(" (에러 코드: ").append(errorCode).append(")");
		}

		return message.toString();
	}

}



