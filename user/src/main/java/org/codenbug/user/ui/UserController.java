package org.codenbug.user.ui;

import org.codenbug.common.AccessToken;
import org.codenbug.common.RsData;
import org.codenbug.common.Util;
import org.codenbug.user.app.UserCommandQueryService;
import org.codenbug.user.app.UserRegisterService;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/users/")
public class UserController {

	private final UserRegisterService userRegisterService;
	private final UserCommandQueryService userQueryService;

	public UserController(UserRegisterService userRegisterService, UserCommandQueryService userQueryService) {
		this.userRegisterService = userRegisterService;
		this.userQueryService = userQueryService;
	}

	@PostMapping("/register")
	public ResponseEntity<RsData<UserId>> register(@RequestBody RegisterRequest request) {
		UserId userId = userRegisterService.register(request);
		return ResponseEntity.ok(new RsData<>("202", "유저 생성 요청이 전송되었습니다.", userId));
	}

	@GetMapping("/me")
	public ResponseEntity<RsData<UserInfo>> getMe(HttpServletRequest request) {
		String userId = request.getHeader("User-Id");
		UserInfo userinfo = userQueryService.findUser(new UserId(userId));
		return ResponseEntity.ok(new RsData<>("200", "User info", userinfo));
	}
}
