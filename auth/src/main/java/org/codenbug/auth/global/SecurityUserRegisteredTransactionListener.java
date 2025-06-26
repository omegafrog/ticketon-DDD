package org.codenbug.auth.global;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecurityUserRegisteredTransactionListener {
	private final KafkaTemplate<String, SecurityUserRegisteredEvent> kafkaTemplate;

	public SecurityUserRegisteredTransactionListener(KafkaTemplate<String, SecurityUserRegisteredEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@TransactionalEventListener
	public void handleSecurityUserRegistrationCompleted(SecurityUserRegisteredEvent event) {
		kafkaTemplate.send("security-user-registered", event);
	}

}
