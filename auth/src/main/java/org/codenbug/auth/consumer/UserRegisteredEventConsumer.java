package org.codenbug.auth.consumer;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.UserId;
import org.codenbug.message.UserRegisteredEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserRegisteredEventConsumer {
	private final SecurityUserRepository repository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public UserRegisteredEventConsumer( SecurityUserRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@KafkaListener(topics = "user-registered", groupId = "security-user-group")
	@Transactional
	public void consume(UserRegisteredEvent event) {
		try {
			SecurityUser securityUser = repository.findSecurityUser(new SecurityUserId(event.getSecurityUserId()))
				.orElseThrow(() -> new EntityNotFoundException("Cannot find SecurityUser entity"));

			securityUser.updateUserId(new UserId(event.getUserId()));
			log.info("Successfully registered security user for userId: {}", event.getSecurityUserId());
		} catch (Exception e) {
		}
	}

}
