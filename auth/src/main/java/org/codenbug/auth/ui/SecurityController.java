package org.codenbug.auth.ui;

import org.codenbug.auth.app.AuthService;
import org.codenbug.common.AccessToken;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.RsData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class SecurityController {

	private final AuthService authService;

	public SecurityController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<RsData<String>> login(@RequestBody LoginRequest request, HttpServletResponse resp) {

		TokenInfo tokenInfo = authService.loginEmail(request);

		resp.setHeader(HttpHeaders.AUTHORIZATION,
			tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue());

		Cookie refreshToken = new Cookie("refreshToken", tokenInfo.getRefreshToken().getValue());
		refreshToken.setPath("/");
		resp.addCookie(refreshToken);

		return ResponseEntity.ok(new RsData<>("200", "login success.",
			tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue()));

	}

}
