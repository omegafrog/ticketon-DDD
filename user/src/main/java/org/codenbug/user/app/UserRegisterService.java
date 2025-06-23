package org.codenbug.user.app;

import org.codenbug.message.UserRegisteredEvent;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegisterService {

	private final UserRepository userRepository;
	private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

	public UserRegisterService(UserRepository userRepository,
		KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
		this.userRepository = userRepository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Transactional("transactionManager")
	public UserId register(RegisterRequest request) {
		// TODO : securityUser 생성 메소드 어떻게 호출

		UserId userId = userRepository.save(
			new User(request.getName(), Sex.valueOf(request.getSex()), request.getPhoneNum(), request.getLocation(),
				request.getAge()));
		UserRegisteredEvent event = new UserRegisteredEvent(userId.getValue(), request.getEmail(),
			request.getPassword(), "USER");
		kafkaTemplate.send("user-registered", event);
		return userId;
	}

	public void cancelUserRegistration(String userId) {
		userRepository.delete(userId);
	}
}
