package org.codenbug.auth.app;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.codenbug.message.SnsUserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecurityUserRegisterTransactionEventListener {
	private final KafkaTemplate<String, SecurityUserRegisteredEvent> kafkaTemplate;

	public SecurityUserRegisterTransactionEventListener(
			KafkaTemplate<String, SecurityUserRegisteredEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@TransactionalEventListener
	public void handleSnsUserRegistrationCompleted(SecurityUserRegisteredEvent event) {
		kafkaTemplate.send("sns-user-registered", event);
	}

}
