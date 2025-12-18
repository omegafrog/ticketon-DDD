package org.codenbug.user.infra;

import org.codenbug.message.UserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecurityUserRegisteredTransactionListener {
  private RabbitTemplate rabbitTemplate;


  public SecurityUserRegisteredTransactionListener(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }


  @TransactionalEventListener
  public void afterTransaction(UserRegisteredEvent event) {
    rabbitTemplate.convertAndSend("user-securityuser-exchanger", "user.created", event);
  }

}
