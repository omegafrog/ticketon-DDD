package org.codenbug.event.ui;

import java.util.List;

import org.codenbug.common.RsData;
import org.codenbug.event.application.RegisterEventService;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.global.NewEventRequest;
import org.codenbug.seat.domain.SeatLayoutId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1")
public class EventController {

	private final RegisterEventService registerEventService;

	public EventController(RegisterEventService registerEventService) {
		this.registerEventService = registerEventService;
	}
	/**
	 * 이벤트 등록 API
	 * @param request 이벤트 등록 요청 DTO
	 * @return 성공 시 RsData<EventRegisterResponse> 포맷으로 응답
	 */
	// @RoleRequired({UserRole.MANAGER, UserRole.ADMIN})
	@PostMapping
	public ResponseEntity<RsData<EventId>> eventRegister(@RequestBody NewEventRequest request,
		@RequestBody SeatLayoutId layoutId) {
		// register event
		EventId eventId = registerEventService.registerNewEvent(request, layoutId);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 등록 성공",
			eventId
		));
	}
}
