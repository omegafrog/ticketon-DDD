package org.codenbug.user.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.codenbug.user.app.AuthenticatedUser;
import org.codenbug.user.app.UserProfileCommandService;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/")
@Tag(name = "User Command", description = "사용자 변경 API")
public class UserCommandController {

	private final UserProfileCommandService userProfileCommandService;

	public UserCommandController(UserProfileCommandService userProfileCommandService) {
		this.userProfileCommandService = userProfileCommandService;
	}

	@Operation(summary = "내 정보 수정", description = "내 정보를 수정합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
		@ApiResponse(responseCode = "401", description = "인증 정보 필요")
	})
	@PutMapping("/me")
	@AuthNeeded
	@RoleRequired(value = {Role.USER})
	public ResponseEntity<RsData<UserInfo>> updateMe(@Valid @RequestBody UpdateUserRequest request) {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		AuthenticatedUser authenticatedUser = AuthenticatedUser.from(userSecurityToken);
		UserInfo updatedUserInfo = userProfileCommandService.updateUser(
			authenticatedUser,
			authenticatedUser.asUserId(),
			request
		);
		return ResponseEntity.ok(new RsData<>("200", "User updated successfully", updatedUserInfo));
	}
}
