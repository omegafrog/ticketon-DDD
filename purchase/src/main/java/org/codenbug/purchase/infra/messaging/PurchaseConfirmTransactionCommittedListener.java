package org.codenbug.purchase.infra.messaging;

import org.codenbug.purchase.app.event.PurchaseConfirmTransactionCommitted;

import org.codenbug.purchase.infra.config.PurchaseRabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PurchaseConfirmTransactionCommittedListener {
  private final RabbitTemplate rabbitTemplate;

  public PurchaseConfirmTransactionCommittedListener(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @TransactionalEventListener(PurchaseConfirmTransactionCommitted.class)
  public void publishMessage(PurchaseConfirmTransactionCommitted event) {
    rabbitTemplate.convertAndSend(PurchaseRabbitMqConfig.PAYMENT_EXCHANGE,
        PurchaseRabbitMqConfig.PAYMENT_CONFIRM_ROUTING_KEY,
        event.getPayloadJson());
  }
}
