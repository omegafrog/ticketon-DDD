package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PurchaseConfirmScheduler {
	private static final int BATCH_SIZE = 50;

	private final ObjectMapper objectMapper;
	private final JpaPurchaseOutboxRepository outboxRepository;
	private final JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;
	private final PurchaseEventAppendService eventAppendService;
	private final PurchaseConfirmWorker confirmWorker;
	private final int maxPublishAttempts;

	public PurchaseConfirmScheduler(ObjectMapper objectMapper,
		JpaPurchaseOutboxRepository outboxRepository,
		JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository,
		PurchaseEventAppendService eventAppendService,
		PurchaseConfirmWorker confirmWorker,
		@Value("${purchase.outbox.max-publish-attempts:3}") int maxPublishAttempts) {
		this.objectMapper = objectMapper;
		this.outboxRepository = outboxRepository;
		this.statusProjectionRepository = statusProjectionRepository;
		this.eventAppendService = eventAppendService;
		this.confirmWorker = confirmWorker;
		this.maxPublishAttempts = maxPublishAttempts;
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
			String purchaseId = extractPurchaseId(msg.getPayloadJson());
			PurchaseConfirmStatus currentStatus = findCurrentStatus(purchaseId);

			if (isTerminalStatus(currentStatus)) {
				msg.markFailedPermanently(LocalDateTime.now(), "non-retryable status: " + currentStatus.name());
				outboxRepository.save(msg);
				continue;
			}

			if (msg.getPublishAttempts() >= maxPublishAttempts) {
				markProjectionFailedByMaxAttempt(purchaseId, msg.getPublishAttempts());
				msg.markFailedPermanently(LocalDateTime.now(), "max publish attempts exceeded");
				outboxRepository.save(msg);
				continue;
			}

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

	private PurchaseConfirmStatus findCurrentStatus(String purchaseId) {
		if (purchaseId == null || purchaseId.isBlank()) {
			return null;
		}
		return statusProjectionRepository.findById(purchaseId)
			.map(projection -> projection.getStatus())
			.orElse(null);
	}

	private boolean isTerminalStatus(PurchaseConfirmStatus status) {
		if (status == null) {
			return false;
		}
		return status == PurchaseConfirmStatus.DONE
			|| status == PurchaseConfirmStatus.FAILED
			|| status == PurchaseConfirmStatus.REJECTED;
	}

	private void markProjectionFailedByMaxAttempt(String purchaseId, int attempts) {
		if (purchaseId == null || purchaseId.isBlank()) {
			return;
		}
		PurchaseConfirmStatus currentStatus = findCurrentStatus(purchaseId);
		if (isTerminalStatus(currentStatus)) {
			return;
		}

		eventAppendService.appendAndUpdateProjection(
			purchaseId,
			"confirm:" + purchaseId,
			PurchaseEventType.PG_CONFIRM_FAILED,
			Map.of("reason", "max_publish_attempts_exceeded", "attempts", attempts),
			PurchaseConfirmStatus.FAILED,
			"max attempts exceeded"
		);
	}

	private String extractPurchaseId(String payloadJson) {
		if (payloadJson == null || payloadJson.isBlank()) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(payloadJson, Map.class);
			Object value = map.get("purchaseId");
			return value == null ? null : value.toString();
		} catch (Exception ignore) {
			return null;
		}
	}
}
