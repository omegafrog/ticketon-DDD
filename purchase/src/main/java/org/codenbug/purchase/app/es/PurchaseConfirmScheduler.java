package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PurchaseConfirmScheduler {
	private static final int BATCH_SIZE = 50;

	private final JpaPurchaseOutboxRepository outboxRepository;
	private final PurchaseConfirmWorker confirmWorker;

	public PurchaseConfirmScheduler(JpaPurchaseOutboxRepository outboxRepository, PurchaseConfirmWorker confirmWorker) {
		this.outboxRepository = outboxRepository;
		this.confirmWorker = confirmWorker;
	}

	@Scheduled(fixedDelayString = "${purchase.outbox.publish-interval-ms:500}")
	public void processPendingConfirms() {
		List<PurchaseOutboxMessage> batch = outboxRepository.findUnpublishedByQueueName(
			PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE,
			PageRequest.of(0, BATCH_SIZE)
		);
		if (batch.isEmpty()) {
			return;
		}

		for (PurchaseOutboxMessage msg : batch) {
			try {
				confirmWorker.process(msg.getMessageId(), msg.getPayloadJson());
				msg.markPublished(LocalDateTime.now());
				outboxRepository.save(msg);
			} catch (Exception e) {
				msg.markPublishAttemptFailed(e.getMessage());
				outboxRepository.save(msg);
			}
		}
	}
}
