package org.codenbug.user.app;

import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.stereotype.Service;

@Service
public class UserRegisterService {

	private final UserRepository userRepository;

	public UserRegisterService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserId register(RegisterRequest request) {
		return userRepository.save(
			new User(request.getName(), Sex.valueOf(request.getSex()), request.getPhoneNum(), request.getLocation(),
				request.getAge()));
	}
}
