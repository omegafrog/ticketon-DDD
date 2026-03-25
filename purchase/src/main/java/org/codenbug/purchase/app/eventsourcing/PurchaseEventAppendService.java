package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.Map;

import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseEventStream;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStreamRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PurchaseEventAppendService {
	private final ObjectMapper objectMapper;
	private final JpaPurchaseEventStreamRepository streamRepository;
	private final JpaPurchaseEventStoreRepository eventStoreRepository;
	private final JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;

	public PurchaseEventAppendService(ObjectMapper objectMapper, JpaPurchaseEventStreamRepository streamRepository,
		JpaPurchaseEventStoreRepository eventStoreRepository,
		JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository) {
		this.objectMapper = objectMapper;
		this.streamRepository = streamRepository;
		this.eventStoreRepository = eventStoreRepository;
		this.statusProjectionRepository = statusProjectionRepository;
	}

	public long appendAndUpdateProjection(String purchaseId, String commandId, PurchaseEventType eventType,
		Map<String, Object> payload, PurchaseConfirmStatus status, String message) {
		PurchaseEventStream stream = streamRepository.findById(purchaseId)
			.orElseGet(() -> streamRepository.save(new PurchaseEventStream(purchaseId)));
		long seq = stream.nextSeq();
		streamRepository.save(stream);

		LocalDateTime now = LocalDateTime.now();
		eventStoreRepository.save(new PurchaseStoredEvent(
			purchaseId,
			seq,
			eventType.name(),
			commandId,
			toJson(payload),
			toJson(Map.of("commandId", commandId)),
			now
		));

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
