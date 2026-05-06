package org.codenbug.purchase.app;

import java.io.IOException;
import java.util.Map;

import org.codenbug.purchase.app.es.PaymentOutboxEventType;
import org.codenbug.purchase.app.es.PurchaseConfirmWorker;
import org.codenbug.purchase.config.PurchaseRabbitMqConfig;
import org.codenbug.purchase.domain.PurchaseId;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.Channel;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentConfirmedConsumer {
  private final PurchaseConfirmWorker confirmWorker;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = { PurchaseRabbitMqConfig.PAYMENT_CONFIRM_QUEUE })
  public void handle(PurchaseConfirmTransactionCommitted message, Channel channel, Message rawMessage) {
    String json = message.getPayloadJson();
    Map<String, Object> valueMap = null;
    try {

      valueMap = objectMapper.readValue(
          json,
          new TypeReference<Map<String, Object>>() {
          });
    } catch (IOException e) {
      e.printStackTrace();
      new RuntimeException(e);
    }

    PurchaseId purchaseId = (PurchaseId) valueMap.get("purchaseId");
    PaymentOutboxEventType eventType = (PaymentOutboxEventType) valueMap.get("eventType");
    String messageId = eventType.toString() + ":" + purchaseId.getValue();
    confirmWorker.process(messageId, json);
  }

}
