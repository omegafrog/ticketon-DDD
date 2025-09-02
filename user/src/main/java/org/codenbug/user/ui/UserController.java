package org.codenbug.user.ui;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.codenbug.user.app.UserQueryService;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/")
@Tag(name = "User", description = "사용자 정보 관리 API")
public class UserController {

	private final UserQueryService userQueryService;

	public UserController( UserQueryService userQueryService) {
		this.userQueryService = userQueryService;
	}



	@GetMapping("/me")
	@AuthNeeded
	@RoleRequired(value={Role.USER, Role.MANAGER})
	public ResponseEntity<RsData<UserInfo>> getMe() {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		UserInfo userinfo = userQueryService.findMe(userSecurityToken, new UserId(userSecurityToken.getUserId()));
		return ResponseEntity.ok(new RsData<>("200", "User info", userinfo));
	}

	@PutMapping("/me")
	@AuthNeeded
	@RoleRequired(value={Role.USER})
	public ResponseEntity<RsData<UserInfo>> updateMe(@RequestBody UpdateUserRequest request) {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		UserInfo updatedUserInfo = userQueryService.updateUser(userSecurityToken, new UserId(userSecurityToken.getUserId()), request);
		return ResponseEntity.ok(new RsData<>("200", "User updated successfully", updatedUserInfo));
	}
}
