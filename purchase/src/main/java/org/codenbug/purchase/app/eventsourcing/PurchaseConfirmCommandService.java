package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.codenbug.purchase.app.PaymentProvider;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseEventStream;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.global.ConfirmPaymentAcceptedResponse;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStreamRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PurchaseConfirmCommandService {
	public static final String CONFIRM_WORK_QUEUE = "purchase.payment.confirm.work";

	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;
	private final JpaPurchaseEventStreamRepository streamRepository;
	private final JpaPurchaseEventStoreRepository eventStoreRepository;
	private final JpaPurchaseOutboxRepository outboxRepository;
	private final JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;

	public PurchaseConfirmCommandService(ObjectMapper objectMapper, PurchaseRepository purchaseRepository,
		JpaPurchaseEventStreamRepository streamRepository, JpaPurchaseEventStoreRepository eventStoreRepository,
		JpaPurchaseOutboxRepository outboxRepository,
		JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository) {
		this.objectMapper = objectMapper;
		this.purchaseRepository = purchaseRepository;
		this.streamRepository = streamRepository;
		this.eventStoreRepository = eventStoreRepository;
		this.outboxRepository = outboxRepository;
		this.statusProjectionRepository = statusProjectionRepository;
	}

	@Transactional
	public ConfirmPaymentAcceptedResponse requestConfirm(ConfirmPaymentRequest request, String userId) {
		String purchaseId = request.getPurchaseId();
		String commandId = "confirm:" + purchaseId;

		Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
			.orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));
		purchase.validate(request.getOrderId(), request.getAmount(), userId);
		if (purchase.getExpectedSalesVersion() == null) {
			throw new IllegalStateException("expectedSalesVersion is not set. Call /payments/init first.");
		}

		boolean alreadyRequested = eventStoreRepository.existsByPurchaseIdAndCommandId(purchaseId, commandId);
		if (!alreadyRequested) {
			PurchaseEventStream stream = streamRepository.findById(purchaseId)
				.orElseGet(() -> streamRepository.save(new PurchaseEventStream(purchaseId)));
			long seq = stream.nextSeq();
			streamRepository.save(stream);

			LocalDateTime now = LocalDateTime.now();
			String payloadJson = toJson(Map.of(
				"purchaseId", purchaseId,
				"userId", userId,
				"eventId", purchase.getEventId(),
				"expectedSalesVersion", purchase.getExpectedSalesVersion(),
				"paymentKey", request.getPaymentKey(),
				"orderId", request.getOrderId(),
				"amount", request.getAmount(),
				"provider", PaymentProvider.from(request.getProvider()).name()
			));
			String metadataJson = toJson(Map.of("commandId", commandId));
			eventStoreRepository.save(new PurchaseStoredEvent(
				purchaseId,
				seq,
				PurchaseEventType.CONFIRM_REQUESTED.name(),
				commandId,
				payloadJson,
				metadataJson,
				now
			));

			PurchaseConfirmStatusProjection projection = statusProjectionRepository.findById(purchaseId)
				.orElseGet(() -> PurchaseConfirmStatusProjection.pending(purchaseId, seq,
					PurchaseEventType.CONFIRM_REQUESTED.name(), now));
			projection.update(org.codenbug.purchase.domain.es.PurchaseConfirmStatus.PENDING, seq,
				PurchaseEventType.CONFIRM_REQUESTED.name(), "accepted", now);
			statusProjectionRepository.save(projection);

			String messageId = UUID.randomUUID().toString();
			String workPayload = toJson(Map.of("purchaseId", purchaseId));
			outboxRepository.save(PurchaseOutboxMessage.of(messageId, CONFIRM_WORK_QUEUE, workPayload, now));
		}

		String statusUrl = "/api/v1/payments/confirm/" + purchaseId + "/status";
		return new ConfirmPaymentAcceptedResponse(purchaseId, "PENDING", statusUrl);
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new IllegalStateException("json serialization failed", e);
		}
	}
}
