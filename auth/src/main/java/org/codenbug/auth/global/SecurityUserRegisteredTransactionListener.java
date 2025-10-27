package org.codenbug.auth.global;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecurityUserRegisteredTransactionListener {
	private final RabbitTemplate rabbitTemplate;


	public SecurityUserRegisteredTransactionListener(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}


	@TransactionalEventListener
	public void handleSecurityUserRegistrationCompleted(SecurityUserRegisteredEvent event) {
		rabbitTemplate.convertAndSend("security-user.created", event);
	}

}
