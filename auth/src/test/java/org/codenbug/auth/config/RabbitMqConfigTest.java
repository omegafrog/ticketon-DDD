package org.codenbug.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.codenbug.auth.consumer.UserRegisteredEventConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RabbitMqConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(RabbitMqConfig.class)
		.withPropertyValues(
			"spring.rabbitmq.host=localhost",
			"spring.rabbitmq.port=5672",
			"spring.rabbitmq.username=root",
			"spring.rabbitmq.password=root"
		);

	@Test
	void 사용자_생성_큐가_리스너_이름과_같은_이름으로_선언된다() {
		contextRunner.run(context -> {
			assertThat(context).hasBean("user-created");
			Queue queue = context.getBean("user-created", Queue.class);

			assertThat(queue.getName()).isEqualTo(listenerQueueName());
		});
	}

	@Test
	void 사용자_생성_관련_큐_선언은_서로_다른_이름을_사용한다() {
		contextRunner.run(context -> assertThat(Arrays.asList(
			context.getBean("user-created", Queue.class).getName(),
			context.getBean("user-created-failed", Queue.class).getName(),
			context.getBean("sns-user-created", Queue.class).getName(),
			context.getBean("security-user-created", Queue.class).getName()
		)).doesNotHaveDuplicates());
	}

	private String listenerQueueName() throws NoSuchMethodException {
		Method consume = UserRegisteredEventConsumer.class.getDeclaredMethod(
			"consume",
			org.codenbug.message.UserRegisteredEvent.class
		);
		RabbitListener listener = consume.getAnnotation(RabbitListener.class);

		return listener.queues()[0];
	}
}
