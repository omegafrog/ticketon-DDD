package org.codenbug.event.ui;

import org.codenbug.common.RsData;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.global.EventSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Validated
public class EventInternalController {
	private final EventRepository eventRepository;

	@GetMapping("/{eventId}/summary")
	public ResponseEntity<RsData<EventSummaryResponse>> getEventSummary(@PathVariable @NotBlank String eventId) {
		Event event = eventRepository.findEvent(new EventId(eventId));
		boolean seatSelectable = Boolean.TRUE.equals(event.getEventInformation().getSeatSelectable());
		String status = event.getEventInformation().getStatus().name();

		EventSummaryResponse response = new EventSummaryResponse(
			event.getEventId().getEventId(),
			event.getSeatLayoutId().getValue(),
			seatSelectable,
			status,
			event.getVersion(),
			event.getSalesVersion(),
			event.getEventInformation().getTitle()
		);
		return ResponseEntity.ok(new RsData<>("200", "이벤트 요약 조회 성공", response));
	}

	@GetMapping("/{eventId}/version-check")
	public ResponseEntity<RsData<Boolean>> validateEventVersion(
		@PathVariable @NotBlank String eventId,
		@RequestParam("version") @NotNull Long version,
		@RequestParam("status") @NotBlank String status) {
		EventStatus eventStatus;
		try {
			eventStatus = EventStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(new RsData<>("400", "유효하지 않은 상태 값입니다.", false));
		}
		boolean isValid = eventRepository.isVersionAndStatusValid(new EventId(eventId), version, eventStatus);
		return ResponseEntity.ok(new RsData<>("200", "이벤트 버전 검증 완료", isValid));
	}
}
