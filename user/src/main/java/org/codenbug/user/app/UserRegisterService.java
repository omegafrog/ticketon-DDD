package org.codenbug.user.app;

import org.codenbug.message.UserRegisteredEvent;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegisterService {

	private final UserRepository userRepository;
	private final ApplicationEventPublisher publisher;

	public UserRegisterService(UserRepository userRepository, ApplicationEventPublisher publisher) {
		this.userRepository = userRepository;
		this.publisher = publisher;
	}

	@Transactional
	public UserId register(RegisterRequest request) {
		UserId userId = userRepository.save(
			new User(request.getName(), Sex.valueOf(request.getSex()), request.getPhoneNum(), request.getLocation(),
				request.getAge()));
		UserRegisteredEvent event = new UserRegisteredEvent(userId.getValue(), request.getEmail(),
			request.getPassword(), "USER");
		// user insert transaction commit 이후 securityUser insert를 위한 이벤트 발행
		publisher.publishEvent(event);
		return userId;
	}

	public void cancelUserRegistration(String userId) {
		userRepository.delete(userId);
	}
}
