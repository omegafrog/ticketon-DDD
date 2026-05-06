package org.codenbug.purchase.app;

import org.codenbug.purchase.config.PurchaseRabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PurchaseConfirmTransactionCommittedListener {
  private final PurchaseEventPublisher eventPublisher;
  private final RabbitTemplate rabbitTemplate;

  public PurchaseConfirmTransactionCommittedListener(PurchaseEventPublisher eventPublisher,
      RabbitTemplate rabbitTemplate) {
    this.eventPublisher = eventPublisher;
    this.rabbitTemplate = rabbitTemplate;
  }

  @TransactionalEventListener(PurchaseConfirmTransactionCommitted.class)
  public void publishMessage(PurchaseConfirmTransactionCommitted event) {
    rabbitTemplate.convertAndSend(PurchaseRabbitMqConfig.PAYMENT_EXCHANGE,
        PurchaseRabbitMqConfig.PAYMENT_CONFIRM_ROUTING_KEY,
        event.getPayloadJson());
  }
}
