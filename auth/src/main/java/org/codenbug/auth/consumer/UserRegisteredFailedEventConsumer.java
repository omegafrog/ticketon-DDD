package org.codenbug.auth.consumer;

import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.message.UserRegisteredFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserRegisteredFailedEventConsumer {
	private final SecurityUserRepository repository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public UserRegisteredFailedEventConsumer( SecurityUserRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@KafkaListener(topics = "user-registered-failed", groupId = "user-registered-failed-group")
	@Transactional
	public void consume(UserRegisteredFailedEvent event) {
		try {
			repository.delete(event.getSecurityUserId());


			log.info("Successfully registered security user for userId: {}", event.getSecurityUserId());
		} catch (Exception e) {
			// TODO : securityUser 생성 이후 User 생성 도중에 SecurityUser row 삭제시 어떻게 할 것인가? 락을 걸어야 하는데?
		}
	}

}
