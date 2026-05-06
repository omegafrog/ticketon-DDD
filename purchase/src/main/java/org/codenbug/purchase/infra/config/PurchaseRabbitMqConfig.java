package org.codenbug.purchase.infra.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PurchaseRabbitMqConfig {

  public static final String PAYMENT_EXCHANGE = "payment.exchange";
  public static final String PAYMENT_CONFIRM_QUEUE = "payment.confirm.queue";
  public static final String PAYMENT_CONFIRM_ROUTING_KEY = "payment.confirm.requested";

  @Bean("purchaseRabbitDirectExchange")
  public DirectExchange paymentExchange() {
    return new DirectExchange(PAYMENT_EXCHANGE, true, false);
  }

  @Bean("purchaseRabbitPaymentConfirmQueue")
  public Queue paymentConfirmQueue() {
    return QueueBuilder
        .durable(PAYMENT_CONFIRM_QUEUE)
        .build();
  }

  @Bean
  public Binding paymentconfirmBinding(
      @Qualifier("purchaseRabbitPaymentConfirmQueue") Queue paymentConfirmQueue,
      @Qualifier("purchaseRabbitDirectExchange") DirectExchange paymentExchange) {
    return BindingBuilder
        .bind(paymentConfirmQueue)
        .to(paymentExchange)
        .with(PAYMENT_CONFIRM_ROUTING_KEY);
  }
}
