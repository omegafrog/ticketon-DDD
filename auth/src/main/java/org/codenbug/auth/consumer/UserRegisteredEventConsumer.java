package org.codenbug.auth.consumer;

import java.time.LocalDateTime;

import org.codenbug.auth.app.AuthService;
import org.codenbug.auth.domain.UserId;
import org.codenbug.common.Role;
import org.codenbug.message.UserRegisteredEvent;
import org.codenbug.message.UserRegisteredFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserRegisteredEventConsumer {

	private final AuthService authService;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public UserRegisteredEventConsumer(AuthService authService, KafkaTemplate<String, Object> kafkaTemplate) {
		this.authService = authService;
		this.kafkaTemplate = kafkaTemplate;
	}

	@KafkaListener(topics = "user-registered", groupId = "security-user-group")
	public void consume(UserRegisteredEvent event) {
		try {

			authService.register(new UserId(event.getUserId()), event.getEmail(), event.getPassword(),
				Role.valueOf(event.getRole()));
			log.info("Successfully registered security user for userId: {}", event.getUserId());
		} catch (Exception e) {
			// log.error("Failed to register security user for userId: {}. Error: {}", event.getUserId(), e.getMessage());
			// // 재시도 또는 실패 처리 로직 추가
			UserRegisteredFailedEvent failedEvent = new UserRegisteredFailedEvent(event.getUserId(), LocalDateTime.now());
			kafkaTemplate.send("user-registered-failed", failedEvent);
		}
	}

}
