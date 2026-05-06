package org.codenbug.user.app;

import org.codenbug.common.Role;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.global.dto.UserInfo;
import org.codenbug.user.ui.UpdateUserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileCommandService {

	private final UserRepository userRepository;

	public UserProfileCommandService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public UserInfo updateUser(AuthenticatedUser authenticatedUser, UserId userId, UpdateUserRequest request) {
		authenticatedUser.verifySelf(userId);
		authenticatedUser.requireRole(Role.USER);
		User user = userRepository.findUser(userId);
		user.update(request.getName(), request.getAge(), request.getLocation(), request.getPhoneNum());
		userRepository.save(user);
		return new UserInfo(user, authenticatedUser.email(), authenticatedUser.role());
	}
}
