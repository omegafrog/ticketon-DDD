package org.codenbug.auth.ui;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.codenbug.auth.app.AuthService;
import org.codenbug.auth.app.OAuthService;
import org.codenbug.auth.domain.RefreshTokenBlackList;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.common.AccessToken;
import org.codenbug.common.RefreshToken;
import org.codenbug.common.RsData;
import org.codenbug.common.TokenInfo;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "인증 및 로그인 API")
@Slf4j
public class SecurityController {

	private final AuthService authService;
	private final OAuthService oAuthService;
	private final RefreshTokenBlackList blackList;

	@Value("${custom.cookie.domain}")
	private String domain;
	@Value("${spring.profiles.active}")
	private String activeProfile;


	public SecurityController(AuthService authService, OAuthService oAuthService, RefreshTokenBlackList blackList) {
		this.authService = authService;
		this.oAuthService = oAuthService;
		this.blackList = blackList;
	}

	@Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "회원가입 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
		@ApiResponse(responseCode = "409", description = "이미 존재하는 사용자")
	})
	@PostMapping("/register")
	public ResponseEntity<RsData<SecurityUserId>> register(
		@Parameter(description = "회원가입 정보", required = true)
		@RequestBody RegisterRequest request) {
		SecurityUserId userId = authService.register(request);
		return ResponseEntity.ok(new RsData<>("202", "유저 생성 요청이 전송되었습니다.", userId));
	}

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 자격증명)"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	@PostMapping("/login")
	public ResponseEntity<RsData<String>> login(
		@Parameter(description = "로그인 정보", required = true)
		@RequestBody LoginRequest request, 
		HttpServletResponse resp) {
		long startTime = System.currentTimeMillis();
		log.info("Login request started for email: {}", request.getEmail());

		try {
			TokenInfo tokenInfo = authService.loginEmail(request);

			resp.setHeader(HttpHeaders.AUTHORIZATION,
				tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue());

			Cookie refreshToken = createRefreshTokenCookie(tokenInfo.getRefreshToken());
			resp.addCookie(refreshToken);

			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			log.info("Login SUCCESS for email: {} - Duration: {}ms", request.getEmail(), duration);

			return ResponseEntity.ok(new RsData<>("200", "login success.",
				tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue()));
		} catch (Exception e) {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			log.error("Login FAILED for email: {} - Duration: {}ms - Error: {}", 
				request.getEmail(), duration, e.getMessage(), e);
			throw e;
		}
	}

	private Cookie createRefreshTokenCookie(RefreshToken refreshToken) {
		Cookie created = new Cookie("refreshToken", refreshToken.getValue());
		if(activeProfile!=null && activeProfile.equals("prod")){
			created.setDomain(domain);
		}
		created.setPath("/");
		created.setMaxAge(60 * 60 * 24 * 7);
		created.setSecure(true);
		created.setHttpOnly(false);
		created.setAttribute("SameSite", "None");
		return created;
	}

	@Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
	})
	@AuthNeeded
	@GetMapping("/logout")
	public ResponseEntity<RsData<Void>> logout(HttpServletRequest req, HttpServletResponse resp) {
		Cookie refreshToken = Arrays.stream(req.getCookies())
			.filter(cookie -> cookie.getName().equals("refreshToken"))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("refreshToken is null."));

		refreshToken.setMaxAge(0);
		resp.addCookie(refreshToken);

		blackList.add(req.getHeader("User-Id"), refreshToken);

		return ResponseEntity.ok(new RsData<>("200", "logout success.", null));
	}

	@Operation(summary = "소셜 로그인 요청", description = "소셜 로그인 페이지로의 리다이렉션 URL을 반환합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "소셜 로그인 URL 반환 성공"),
		@ApiResponse(responseCode = "400", description = "지원하지 않는 소셜 로그인 타입")
	})
	@GetMapping(value = "/social/{socialLoginType}")
	public ResponseEntity<String> request(
		@Parameter(description = "소셜 로그인 타입 (google, kakao)", required = true)
		@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType) {

		String redirectURL = authService.request(socialLoginType);

		return ResponseEntity.ok(redirectURL);  // 리다이렉션 URL을 응답으로 반환
	}

	@Operation(summary = "소셜 로그인 콜백", description = "소셜 로그인 콜백을 처리하고 JWT 토큰을 발급합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "소셜 로그인 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 인증 코드"),
		@ApiResponse(responseCode = "500", description = "소셜 로그인 처리 중 오류")
	})
	@GetMapping(value = "/social/{socialLoginType}/callback")
	public ResponseEntity<RsData<String>> callback(
		@Parameter(description = "소셜 로그인 타입", required = true)
		@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
		@Parameter(description = "소셜 로그인 인증 코드", required = true)
		@RequestParam(name = "code") String code,
		@Parameter(description = "리다이렉션 URL", required = false)
		@RequestParam(name = "redirectUrl", required = false) String redirectUrl,
		HttpServletResponse response) {

		log.info(">> 소셜 로그인 API 서버로부터 받은 code :: {}", code);
		log.info(">> 콜백 시 사용된 리다이렉트 URL: {}", redirectUrl);

		try {

			String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
			// 액세스 토큰을 통해 사용자 정보를 받아오고 JWT 토큰 생성 (리다이렉트 URL 전달)
			SocialLoginResponse userResponse = oAuthService.requestAccessTokenAndSaveUser(socialLoginType, decodedCode);

			// 쿠키에 토큰 저장 (UserController.login 메서드와 유사하게)
			AccessToken accessToken = userResponse.tokenInfo().getAccessToken();
			response.setHeader("Authorization", accessToken.getType() + " " + accessToken.getRawValue());
			Cookie refreshTokenCookie = createRefreshTokenCookie(userResponse.tokenInfo().getRefreshToken());

			response.addCookie(refreshTokenCookie);

			return ResponseEntity.ok(
				new RsData<>("200-SUCCESS", "소셜 로그인 성공", accessToken.getType() + " " + accessToken.getRawValue()));

		} catch (Exception e) {
			log.error(">> 소셜 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new RsData<>("500-INTERNAL_SERVER_ERROR", "소셜 로그인 처리 중 오류가 발생했습니다.", null));
		}
	}

}
