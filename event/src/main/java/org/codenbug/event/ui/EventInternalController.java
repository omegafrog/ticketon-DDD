package org.codenbug.event.ui;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.global.EventSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {
	private final EventRepository eventRepository;

	@GetMapping("/{eventId}/summary")
	public ResponseEntity<EventSummaryResponse> getEventSummary(@PathVariable String eventId) {
		Event event = eventRepository.findEvent(new EventId(eventId));
		boolean seatSelectable = Boolean.TRUE.equals(event.getEventInformation().getSeatSelectable());
		String status = event.getEventInformation().getStatus().name();

		EventSummaryResponse response = new EventSummaryResponse(
			event.getEventId().getEventId(),
			event.getSeatLayoutId().getValue(),
			seatSelectable,
			status,
			event.getVersion(),
			event.getEventInformation().getTitle()
		);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{eventId}/version-check")
	public ResponseEntity<Boolean> validateEventVersion(
		@PathVariable String eventId,
		@RequestParam("version") Long version,
		@RequestParam("status") String status) {
		EventStatus eventStatus;
		try {
			eventStatus = EventStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.ok(false);
		}
		boolean isValid = eventRepository.isVersionAndStatusValid(new EventId(eventId), version, eventStatus);
		return ResponseEntity.ok(isValid);
	}
}
