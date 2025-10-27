package org.codenbug.auth.app;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecurityUserRegisterTransactionEventListener {
  private final RabbitTemplate rabbitTemplate;


  public SecurityUserRegisterTransactionEventListener(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }


  @TransactionalEventListener
  public void handleSecurityUserRegistrationCompleted(SecurityUserRegisteredEvent event) {
    rabbitTemplate.convertAndSend("user-securityuser-exchanger", "security-user.created", event);
  }

}
