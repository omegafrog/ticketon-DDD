package org.codenbug.user.app;

import org.codenbug.message.UserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegistrationEventListener {
	private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

	public UserRegistrationEventListener(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@TransactionalEventListener
	public void handleUserRegistrationCompleted(UserRegisteredEvent event) {
		kafkaTemplate.send("user-registered", event);
	}

}
