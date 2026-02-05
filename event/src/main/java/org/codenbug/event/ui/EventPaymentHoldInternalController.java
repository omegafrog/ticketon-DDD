package org.codenbug.event.ui;

import org.codenbug.common.RsData;
import org.codenbug.event.application.EventPaymentHoldService;
import org.codenbug.event.domain.PaymentHoldRejectedException;
import org.codenbug.event.global.EventPaymentHoldCreateRequest;
import org.codenbug.event.global.EventPaymentHoldCreateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events/{eventId}/payment-holds")
public class EventPaymentHoldInternalController {
	private final EventPaymentHoldService holdService;

	public EventPaymentHoldInternalController(EventPaymentHoldService holdService) {
		this.holdService = holdService;
	}

	@PostMapping
	public ResponseEntity<RsData<EventPaymentHoldCreateResponse>> createHold(@PathVariable String eventId,
		@RequestBody EventPaymentHoldCreateRequest request) {
		try {
			EventPaymentHoldCreateResponse response = holdService.createHold(
				eventId,
				request.getExpectedSalesVersion(),
				request.getTtlSeconds(),
				request.getPurchaseId()
			);
			return ResponseEntity.ok(new RsData<>("200", "payment hold created", response));
		} catch (PaymentHoldRejectedException ex) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new RsData<>(HttpStatus.CONFLICT.toString(), ex.getMessage(), null));
		}
	}

	@PostMapping("/{holdToken}/consume")
	public ResponseEntity<RsData<Void>> consume(@PathVariable String eventId, @PathVariable String holdToken) {
		holdService.consumeHold(eventId, holdToken);
		return ResponseEntity.ok(new RsData<>("200", "payment hold consumed", null));
	}

	@PostMapping("/{holdToken}/release")
	public ResponseEntity<RsData<Void>> release(@PathVariable String eventId, @PathVariable String holdToken) {
		holdService.releaseHold(eventId, holdToken);
		return ResponseEntity.ok(new RsData<>("200", "payment hold released", null));
	}
}
