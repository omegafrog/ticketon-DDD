package org.codenbug.user.consumer;

import org.codenbug.message.SnsUserRegisteredEvent;
import org.codenbug.message.UserRegisteredFailedEvent;
import org.codenbug.user.app.UserRegisterService;
import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SnsUserRegisteredConsumer {

	private final UserRegisterService userRegisterService;

	public SnsUserRegisteredConsumer(UserRegisterService userRegisterService) {
		this.userRegisterService = userRegisterService;
	}

	@KafkaListener(topics = "sns-user-registered", groupId = "sns-user-registered")
	@Transactional
	public void consume(SnsUserRegisteredEvent event) {
		try {
			// securityUser로 user를 생성
			userRegisterService.register(
				new RegisterRequest(new SecurityUserId(event.getSecurityUserId()), event.getName(), event.getAge(),
					event.getSex(),null, null));
		} catch (Exception e) {
			log.error("Failed to create User for securityUserId: {}", event.getSecurityUserId(), e);
			// TODO: securityUser이벤트로 user 생성 실패시 보상 트랜잭션 필요
		}

	}
}
