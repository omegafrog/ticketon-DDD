package org.codenbug.user.infra.consumer;

import org.codenbug.message.SnsUserRegisteredEvent;
import org.codenbug.message.UserRegisteredEvent;
<<<<<<< HEAD
import org.codenbug.user.app.UserRegisterService;
=======
import org.codenbug.user.app.UserCommandService;
>>>>>>> tmp
import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SnsUserRegisteredConsumer {

	private final UserCommandService userCommandService;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public SnsUserRegisteredConsumer(UserCommandService userCommandService,
		KafkaTemplate<String, Object> kafkaTemplate) {
		this.userCommandService = userCommandService;
		this.kafkaTemplate = kafkaTemplate;
	}

	@KafkaListener(topics = "sns-user-registered", groupId = "sns-user-registered")
	@Transactional
	public void consume(SnsUserRegisteredEvent event) {
		try {
			// securityUser로 user를 생성
			UserId userId = userCommandService.register(
				new RegisterRequest(new SecurityUserId(event.getSecurityUserId()), event.getName(), event.getAge(),
					event.getSex(), null, null));
			kafkaTemplate.send("user-registered",
				new UserRegisteredEvent(event.getSecurityUserId(), userId.getValue()));
		} catch (Exception e) {
			log.error("Failed to create User for securityUserId: {}", event.getSecurityUserId(), e);
			// TODO: securityUser이벤트로 user 생성 실패시 보상 트랜잭션 필요
		}

	}
}
