package org.codenbug.seat.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	@Bean
	public Queue seatPurchasedQueue() {
		return new Queue("seat-purchased");
	}

}
