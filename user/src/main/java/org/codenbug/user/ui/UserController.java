package org.codenbug.user.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.codenbug.user.app.UserCommandQueryService;
import org.codenbug.user.app.UserRegisterService;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/")
public class UserController {

	private final UserRegisterService userRegisterService;
	private final UserCommandQueryService userQueryService;

	public UserController(UserRegisterService userRegisterService, UserCommandQueryService userQueryService) {
		this.userRegisterService = userRegisterService;
		this.userQueryService = userQueryService;
	}



	@GetMapping("/me")
	@AuthNeeded
	@RoleRequired(value={Role.USER})
	public ResponseEntity<RsData<UserInfo>> getMe() {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		UserInfo userinfo = userQueryService.findUser(userSecurityToken, new UserId(userSecurityToken.getUserId()));
		return ResponseEntity.ok(new RsData<>("200", "User info", userinfo));
	}
}
