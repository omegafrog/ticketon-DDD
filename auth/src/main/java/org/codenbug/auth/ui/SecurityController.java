package org.codenbug.auth.ui;

import java.util.Arrays;

import org.codenbug.auth.app.AuthService;
import org.codenbug.auth.domain.RefreshTokenBlackList;
import org.codenbug.common.AccessToken;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.RsData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class SecurityController {

	private final AuthService authService;
	private final RefreshTokenBlackList blackList;

	public SecurityController(AuthService authService, RefreshTokenBlackList blackList) {
		this.authService = authService;
		this.blackList = blackList;
	}

	@PostMapping("/login")
	public ResponseEntity<RsData<String>> login(@RequestBody LoginRequest request, HttpServletResponse resp) {

		TokenInfo tokenInfo = authService.loginEmail(request);

		resp.setHeader(HttpHeaders.AUTHORIZATION,
			tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue());

		Cookie refreshToken = new Cookie("refreshToken", tokenInfo.getRefreshToken().getValue());
		refreshToken.setPath("/");
		refreshToken.setMaxAge(60*60*24*7);
		resp.addCookie(refreshToken);

		return ResponseEntity.ok(new RsData<>("200", "login success.",
			tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue()));
	}

	@GetMapping("/logout")
	public ResponseEntity<RsData<Void>> logout(HttpServletRequest req, HttpServletResponse resp){
		Cookie refreshToken = Arrays.stream(req.getCookies())
			.filter(cookie -> cookie.getName().equals("refreshToken"))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("refreshToken is null."));

		refreshToken.setMaxAge(0);
		resp.addCookie(refreshToken);

		blackList.add(req.getHeader("User-Id") ,refreshToken);



		return ResponseEntity.ok(new RsData<>("200", "logout success.", null));
	}

}
