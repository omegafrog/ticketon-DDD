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
			new User(request.getName(), request.getSex() == null ? null : Sex.valueOf(request.getSex()),
				request.getPhoneNum(), request.getLocation(),
				request.getAge(), request.getSecurityUserId()));
		
		// Publish event after transaction success
		UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(
			request.getSecurityUserId().getValue(),
			userId.getValue()
		);
		publisher.publishEvent(userRegisteredEvent);
		
		return userId;
	}

	public void cancelUserRegistration(String userId) {
		userRepository.delete(userId);
	}
}
