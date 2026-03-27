package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codenbug.purchase.app.PaymentProvider;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.global.ConfirmPaymentAcceptedResponse;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PurchaseConfirmCommandService {
	public static final String CONFIRM_WORK_QUEUE = "purchase.payment.confirm.work";

	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;
	private final JpaPurchaseEventStoreRepository eventStoreRepository;
	private final JpaPurchaseOutboxRepository outboxRepository;
	private final JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;

	public PurchaseConfirmCommandService(ObjectMapper objectMapper, PurchaseRepository purchaseRepository,
			JpaPurchaseEventStoreRepository eventStoreRepository,
			JpaPurchaseOutboxRepository outboxRepository,
			JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository) {
		this.objectMapper = objectMapper;
		this.purchaseRepository = purchaseRepository;
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

		boolean alreadyRequested = eventStoreRepository.existsByPurchaseIdAndCommandId(purchaseId, commandId);
		if (!alreadyRequested) {
			Long expectedSalesVersion = resolveExpectedSalesVersionFromInitEvent(purchaseId, purchase);
			if (expectedSalesVersion == null) {
				throw new IllegalStateException("expectedSalesVersion is missing in init context. purchaseId=" + purchaseId);
			}

			LocalDateTime now = LocalDateTime.now();
			String payloadJson = toJson(Map.of(
				"purchaseId", purchaseId,
				"userId", userId,
				"eventId", purchase.getEventId(),
				"expectedSalesVersion", expectedSalesVersion,
				"paymentKey", request.getPaymentKey(),
				"orderId", request.getOrderId(),
				"amount", request.getAmount(),
				"provider", PaymentProvider.from(request.getProvider()).name()
			));
			String metadataJson = toJson(Map.of("commandId", commandId));
			PurchaseStoredEvent savedEvent = eventStoreRepository.save(new PurchaseStoredEvent(
				purchaseId,
				PurchaseEventType.CONFIRM_REQUESTED.name(),
				commandId,
				payloadJson,
				metadataJson,
				now
			));
			Long generatedEventId = savedEvent.getId();
			if (generatedEventId == null) {
				throw new IllegalStateException("generated event id is missing");
			}
			long seq = generatedEventId;

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

	private Long resolveExpectedSalesVersionFromInitEvent(String purchaseId, Purchase purchase) {
		List<PurchaseStoredEvent> events = eventStoreRepository.findByPurchaseIdOrderByIdAsc(purchaseId);
		for (int i = events.size() - 1; i >= 0; i--) {
			PurchaseStoredEvent event = events.get(i);
			if (!PurchaseEventType.PAYMENT_INITIATED.name().equals(event.getEventType())) {
				continue;
			}
			Long version = readExpectedSalesVersion(event.getPayloadJson());
			if (version != null) {
				return version;
			}
		}

		return purchase.getExpectedSalesVersion();
	}

	private Long readExpectedSalesVersion(String payloadJson) {
		if (payloadJson == null || payloadJson.isBlank()) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
			Object value = payload.get("expectedSalesVersion");
			if (value == null) {
				return null;
			}
			if (value instanceof Number number) {
				return number.longValue();
			}
			return Long.valueOf(String.valueOf(value));
		} catch (Exception ignore) {
			return null;
		}
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new IllegalStateException("json serialization failed", e);
		}
	}
}
