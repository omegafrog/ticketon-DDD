package org.codenbug.user.app;

import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.global.dto.UserInfo;
import org.springframework.stereotype.Service;

@Service
public class UserCommandQueryService {

	private final UserRepository userRepository;

	public UserCommandQueryService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserInfo findUser(UserId userId){
		User user = userRepository.findUser(userId);
		return new UserInfo(user);
	}
}
