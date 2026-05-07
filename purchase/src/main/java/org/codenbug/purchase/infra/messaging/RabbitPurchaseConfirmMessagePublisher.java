package org.codenbug.purchase.infra.messaging;

import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmMessagePublisher;
import org.codenbug.purchase.infra.config.PurchaseRabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitPurchaseConfirmMessagePublisher implements PurchaseConfirmMessagePublisher {
  private final RabbitTemplate rabbitTemplate;

  public RabbitPurchaseConfirmMessagePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publish(PurchaseOutboxMessage message) {
    rabbitTemplate.convertAndSend(PurchaseRabbitMqConfig.PAYMENT_EXCHANGE,
        PurchaseRabbitMqConfig.PAYMENT_CONFIRM_ROUTING_KEY,
        message.getPayloadJson());
  }
}
