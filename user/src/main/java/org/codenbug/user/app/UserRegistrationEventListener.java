package org.codenbug.user.app;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegistrationEventListener {
	private final KafkaTemplate<String, SecurityUserRegisteredEvent> kafkaTemplate;

	public UserRegistrationEventListener(KafkaTemplate<String, SecurityUserRegisteredEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@TransactionalEventListener
	public void handleUserRegistrationCompleted(SecurityUserRegisteredEvent event) {
		kafkaTemplate.send("user-registered", event);
	}

}
