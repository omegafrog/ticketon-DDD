package org.codenbug.user.infra.consumer;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.codenbug.message.UserRegisteredEvent;
import org.codenbug.message.UserRegisteredFailedEvent;
import org.codenbug.user.app.UserRegisterService;
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
public class SecurityUserRegisteredConsumer {

	private final UserRegisterService userRegisterService;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public SecurityUserRegisteredConsumer(UserRegisterService userRegisterService,
		KafkaTemplate<String, Object> kafkaTemplate) {
		this.userRegisterService = userRegisterService;
		this.kafkaTemplate = kafkaTemplate;
	}

	@KafkaListener(topics = "security-user-registered", groupId = "security-user-registered-group")
	@Transactional
	public void consume(SecurityUserRegisteredEvent event) {
		try {
			// securityUser로 user를 생성
			SecurityUserId securityUserId = new SecurityUserId(event.getSecurityUserId());
			UserId userId = userRegisterService.register(
				new RegisterRequest(securityUserId, event.getName(), event.getAge(),
					event.getSex(),
					event.getPhoneNum(), event.getLocation()));
			// userId를 securityUser에 추가하도록 이벤트 발행
			kafkaTemplate.send("user-registered",
				new UserRegisteredEvent(securityUserId.getValue(), userId.getValue()));
		} catch (Exception e) {
			log.error("Failed to create User for securityUserId: {}", event.getSecurityUserId(), e);
			// TODO: securityUser이벤트로 user 생성 실패시 보상 트랜잭션 필요
			kafkaTemplate.send("user-registered-failed", new UserRegisteredFailedEvent(event.getSecurityUserId()));
		}

	}
}
