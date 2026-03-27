package org.codenbug.user.app;

import org.codenbug.securityaop.aop.UserSecurityToken;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;
import org.codenbug.user.query.UserViewRepository;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {

	private final UserViewRepository userViewRepository;

	public UserQueryService(UserViewRepository userViewRepository) {
		this.userViewRepository = userViewRepository;
	}

	public UserInfo findMe(UserSecurityToken token, UserId userId){
		User user = userViewRepository.findUserById(userId);
		return new UserInfo(user, token.getEmail(), token.getRole());
	}
}
