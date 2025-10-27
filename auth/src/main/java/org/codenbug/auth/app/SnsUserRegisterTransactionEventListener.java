package org.codenbug.auth.app;

import org.codenbug.message.SnsUserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SnsUserRegisterTransactionEventListener {

  private final RabbitTemplate rabbitTemplate;

  public SnsUserRegisterTransactionEventListener(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @TransactionalEventListener
  public void handleSnsUserRegistrationCompleted(SnsUserRegisteredEvent event) {
    rabbitTemplate.convertAndSend("user-securityuser-exchanger", "sns-user.created", event);
  }

}
