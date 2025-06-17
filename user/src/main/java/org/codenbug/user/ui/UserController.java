package org.codenbug.user.ui;

import org.codenbug.common.RsData;
import org.codenbug.user.app.UserRegisterService;
import org.codenbug.user.domain.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/")
public class UserController {

	private final UserRegisterService userRegisterService;

	public UserController(UserRegisterService userRegisterService) {
		this.userRegisterService = userRegisterService;
	}

	@PostMapping("/register")
	public ResponseEntity<RsData<UserId>> register(@RequestBody RegisterRequest request) {
		UserId userId = userRegisterService.register(request);
		return ResponseEntity.ok(new RsData<>("200", "User register success.", userId));
	}
}
