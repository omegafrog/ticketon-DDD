package org.codenbug.auth.app;

import org.codenbug.message.SnsUserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SnsUserRegisterTransactionEventListener {
	private final KafkaTemplate<String, SnsUserRegisteredEvent> kafkaTemplate;

	public SnsUserRegisterTransactionEventListener(
			KafkaTemplate<String, SnsUserRegisteredEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@TransactionalEventListener
	public void handleSnsUserRegistrationCompleted(SnsUserRegisteredEvent event) {
		kafkaTemplate.send("sns-user-registered", event);
	}

}
