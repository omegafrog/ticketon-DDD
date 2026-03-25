package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOutboxPublisher {
	private static final int BATCH_SIZE = 50;

	private final RabbitTemplate rabbitTemplate;
	private final JpaPurchaseOutboxRepository outboxRepository;

	public PurchaseOutboxPublisher(RabbitTemplate rabbitTemplate, JpaPurchaseOutboxRepository outboxRepository) {
		this.rabbitTemplate = rabbitTemplate;
		this.outboxRepository = outboxRepository;
	}

	@Scheduled(fixedDelayString = "${purchase.outbox.publish-interval-ms:500}")
	public void publish() {
		List<PurchaseOutboxMessage> batch = outboxRepository.findUnpublished(PageRequest.of(0, BATCH_SIZE));
		if (batch.isEmpty()) {
			return;
		}

		for (PurchaseOutboxMessage msg : batch) {
			try {
				MessageProperties props = new MessageProperties();
				props.setMessageId(msg.getMessageId());
				props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
				Message message = new Message(msg.getPayloadJson().getBytes(java.nio.charset.StandardCharsets.UTF_8), props);
				rabbitTemplate.send(msg.getQueueName(), message);

				msg.markPublished(LocalDateTime.now());
				outboxRepository.save(msg);
			} catch (Exception e) {
				msg.markPublishAttemptFailed(e.getMessage());
				outboxRepository.save(msg);
			}
		}
	}
}
