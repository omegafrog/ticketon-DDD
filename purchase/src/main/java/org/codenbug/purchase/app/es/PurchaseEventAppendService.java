package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.Map;

import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PurchaseEventAppendService {
	private final ObjectMapper objectMapper;
	private final JpaPurchaseEventStoreRepository eventStoreRepository;
	private final JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;

	public PurchaseEventAppendService(ObjectMapper objectMapper, JpaPurchaseEventStoreRepository eventStoreRepository,
			JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository) {
		this.objectMapper = objectMapper;
		this.eventStoreRepository = eventStoreRepository;
		this.statusProjectionRepository = statusProjectionRepository;
	}

	public long appendAndUpdateProjection(String purchaseId, String commandId, PurchaseEventType eventType,
			Map<String, Object> payload, PurchaseConfirmStatus status, String message) {
		LocalDateTime now = LocalDateTime.now();
		PurchaseStoredEvent savedEvent = eventStoreRepository.save(new PurchaseStoredEvent(
				purchaseId,
				eventType.name(),
				commandId,
				toJson(payload),
				toJson(Map.of("commandId", commandId)),
				now
		));
		Long generatedEventId = savedEvent.getId();
		if (generatedEventId == null) {
			throw new IllegalStateException("generated event id is missing");
		}
		long seq = generatedEventId;

		PurchaseConfirmStatusProjection proj = statusProjectionRepository.findById(purchaseId)
				.orElseGet(() -> PurchaseConfirmStatusProjection.pending(purchaseId, seq, eventType.name(), now));
		proj.update(status, seq, eventType.name(), message, now);
		statusProjectionRepository.save(proj);
		return seq;
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new IllegalStateException("json serialization failed", e);
		}
	}
}
