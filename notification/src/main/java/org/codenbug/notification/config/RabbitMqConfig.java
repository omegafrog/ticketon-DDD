package org.codenbug.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("notificationRabbitMqConfig")
public class RabbitMqConfig {

	@Bean
	public Queue paymentCompletedQueue() {
		return new Queue("payment.completed");
	}

	@Bean
	public Queue refundCompletedQueue() {
		return new Queue("refund.completed");
	}

	@Bean
	public Queue notificationRefundCompletedQueue() {
		return new Queue("notification.refund.completed");
	}

	@Bean
	public Queue notificationManagerRefundCompletedQueue() {
		return new Queue("notification.manager.refund.completed");
	}

}
