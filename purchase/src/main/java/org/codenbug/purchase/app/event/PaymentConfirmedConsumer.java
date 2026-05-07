package org.codenbug.purchase.app.event;

import java.io.IOException;
import java.util.Map;

import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.app.command.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.command.es.PurchaseConfirmWorker;
import org.codenbug.purchase.infra.config.PurchaseRabbitMqConfig;
import org.codenbug.purchase.domain.PurchaseId;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentConfirmedConsumer {
  private final PurchaseConfirmWorker confirmWorker;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = { PurchaseRabbitMqConfig.PAYMENT_CONFIRM_QUEUE })
  public void handle(String json) {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("blank payment confirm message");
    }

    Map<String, Object> valueMap;
    try {
      valueMap = objectMapper.readValue(
          json,
          new TypeReference<Map<String, Object>>() {
          });
    } catch (IOException e) {
      throw new IllegalArgumentException("invalid payment confirm message", e);
    }

    PurchaseId purchaseId = new PurchaseId(requiredString(valueMap, "purchaseId"));
    PaymentOutboxEventType eventType = parseEventType(requiredString(valueMap, "eventType"));
    String messageId = eventType == PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED
        ? PurchaseConfirmCommandService.confirmCommandId(purchaseId)
        : eventType.value + ":" + purchaseId.getValue();
    confirmWorker.process(messageId, json);
  }

  private String requiredString(Map<String, Object> valueMap, String fieldName) {
    Object value = valueMap.get(fieldName);
    if (value == null || value.toString().isBlank()) {
      throw new IllegalArgumentException("payment confirm message missing " + fieldName);
    }
    return value.toString();
  }

  private PaymentOutboxEventType parseEventType(String value) {
    try {
      return PaymentOutboxEventType.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("invalid payment confirm eventType", e);
    }
  }

}
