package org.codenbug.user.consumer;

import org.codenbug.message.UserRegisteredFailedEvent;
import org.codenbug.user.app.UserRegisterService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SecurityUserRegisteredFailedConsumer {

	private final UserRegisterService userRegisterService;

	public SecurityUserRegisteredFailedConsumer(UserRegisterService userRegisterService) {
		this.userRegisterService = userRegisterService;
	}

	@KafkaListener(topics = "user-registered-failed", groupId = "user-registered-failed")
	@Transactional
	public void consume(UserRegisteredFailedEvent event){
		log.warn("Received user registration failure event for userId: {}. Starting compensating transaction.", event.getUserId());
		try {
			// 보상 트랜잭션: 생성되었던 User를 삭제합니다.
			userRegisterService.cancelUserRegistration(event.getUserId());
			log.info("Successfully compensated and deleted user for userId: {}", event.getUserId());
		} catch (Exception e) {
			log.error("Failed to execute compensating transaction for userId: {}", event.getUserId(), e);
			// 보상 트랜잭션 실패 시에는 심각한 문제이므로 별도의 모니터링/알림이 필요할 수 있습니다.
		}

	}
}
