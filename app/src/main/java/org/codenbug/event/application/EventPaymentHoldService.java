package org.codenbug.event.application;

import java.time.LocalDateTime;
import java.util.UUID;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventPaymentHold;
import org.codenbug.event.domain.EventPaymentHoldStatus;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.PaymentHoldRejectedException;
import org.codenbug.event.global.EventPaymentHoldCreateResponse;
import org.codenbug.event.infra.JpaEventPaymentHoldRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class EventPaymentHoldService {
	private final EventRepository eventRepository;
	private final JpaEventPaymentHoldRepository holdRepository;

	public EventPaymentHoldService(EventRepository eventRepository, JpaEventPaymentHoldRepository holdRepository) {
		this.eventRepository = eventRepository;
		this.holdRepository = holdRepository;
	}

	@Transactional
	public EventPaymentHoldCreateResponse createHold(String eventId, Long expectedSalesVersion, Integer ttlSeconds,
		String purchaseId) {
		if (ttlSeconds == null || ttlSeconds <= 0) {
			throw new IllegalArgumentException("ttlSeconds must be positive");
		}
		if (purchaseId == null || purchaseId.isBlank()) {
			throw new IllegalArgumentException("purchaseId is required");
		}

		LocalDateTime now = LocalDateTime.now();
		Event event = eventRepository.findEventForUpdate(new EventId(eventId));

		if (event.getMetaData() == null || Boolean.TRUE.equals(event.getMetaData().getDeleted())) {
			throw new PaymentHoldRejectedException("Event is deleted");
		}
		if (event.getEventInformation().getStatus() != EventStatus.OPEN) {
			throw new PaymentHoldRejectedException("Event is not purchasable");
		}
		if (expectedSalesVersion == null) {
			throw new IllegalArgumentException("expectedSalesVersion is required");
		}
		if (!expectedSalesVersion.equals(event.getSalesVersion())) {
			throw new PaymentHoldRejectedException("Event payment-relevant fields changed");
		}

		return holdRepository
			.findActiveHoldForPurchase(eventId, purchaseId, EventPaymentHoldStatus.ACTIVE, now)
			.map(h -> new EventPaymentHoldCreateResponse(h.getHoldToken(), h.getExpiresAt(), event.getSalesVersion()))
			.orElseGet(() -> {
				String token = UUID.randomUUID().toString();
				LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);
				holdRepository.save(EventPaymentHold.active(token, eventId, purchaseId, expiresAt, now));
				return new EventPaymentHoldCreateResponse(token, expiresAt, event.getSalesVersion());
			});
	}

	@Transactional
	public void consumeHold(String eventId, String holdToken) {
		EventPaymentHold hold = holdRepository.findById(holdToken)
			.orElseThrow(() -> new IllegalArgumentException("hold not found"));
		if (!eventId.equals(hold.getEventId())) {
			throw new IllegalArgumentException("hold does not belong to event");
		}
		hold.consume(LocalDateTime.now());
	}

	@Transactional
	public void releaseHold(String eventId, String holdToken) {
		EventPaymentHold hold = holdRepository.findById(holdToken)
			.orElseThrow(() -> new IllegalArgumentException("hold not found"));
		if (!eventId.equals(hold.getEventId())) {
			throw new IllegalArgumentException("hold does not belong to event");
		}
		hold.release(LocalDateTime.now());
	}

	@Transactional
	public void validateNoActiveHoldForCoreChange(String eventId) {
		boolean exists = holdRepository.existsActiveHold(eventId, EventPaymentHoldStatus.ACTIVE, LocalDateTime.now());
		if (exists) {
			throw new PaymentHoldRejectedException("Event has active payment holds");
		}
	}

	@Transactional
	public boolean hasActiveHold(String eventId) {
		return holdRepository.existsActiveHold(eventId, EventPaymentHoldStatus.ACTIVE, LocalDateTime.now());
	}
}
