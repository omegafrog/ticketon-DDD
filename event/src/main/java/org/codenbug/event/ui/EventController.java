package org.codenbug.event.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.event.application.FindEventService;
import org.codenbug.event.application.RegisterEventService;
import org.codenbug.event.application.UpdateEventService;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.NewEventRequest;
import org.codenbug.event.global.UpdateEventRequest;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

	private final RegisterEventService registerEventService;
	private final UpdateEventService updateEventService;
	private final FindEventService findEventService;

	public EventController(RegisterEventService registerEventService, UpdateEventService updateEventService,
		FindEventService findEventService) {
		this.registerEventService = registerEventService;
		this.updateEventService = updateEventService;
		this.findEventService = findEventService;
	}

	/**
	 * 이벤트 등록 API
	 * @param request 이벤트 등록 요청 DTO
	 * @return 성공 시 RsData<EventRegisterResponse> 포맷으로 응답
	 */
	@AuthNeeded
	@RoleRequired(Role.MANAGER)
	@PostMapping
	public ResponseEntity<RsData<EventId>> eventRegister(@RequestBody NewEventRequest request) {
		// register seat

		// register event
		EventId eventId = registerEventService.registerNewEvent(request);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 등록 성공",
			eventId
		));
	}

	@AuthNeeded
	@RoleRequired({Role.MANAGER, Role.ADMIN})
	@PutMapping("/{eventId}")
	public ResponseEntity<RsData<EventId>> updateEvent(
		@PathVariable String eventId,
		@RequestBody UpdateEventRequest request
	) {
		updateEventService.updateEvent(new EventId(eventId), request);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 수정 성공",
			new EventId(eventId)
		));
	}

	@AuthNeeded
	@RoleRequired({Role.MANAGER, Role.ADMIN})
	@DeleteMapping("/{eventId}")
	public ResponseEntity<RsData<Void>> deleteEvent(@PathVariable String eventId) {
		updateEventService.deleteEvent(new EventId(eventId));
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 삭제 성공",
			null
		));
	}

	@GetMapping("/{eventId}")
	public ResponseEntity<RsData<EventInfoResponse>> getEvent(@PathVariable String eventId){
		EventInfoResponse response = findEventService.findEvent(new EventId(eventId));
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 조회 성공",
			response
		));
	}

	@AuthNeeded
	@RoleRequired({Role.MANAGER, Role.ADMIN})
	@PatchMapping("/{eventId}")
	public ResponseEntity<RsData<EventId>> changeStatus(@PathVariable String eventId, @RequestParam(name = "status") String status){
		updateEventService.changeStatus(eventId, status);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 상태 변경 성공",
			new EventId(eventId)
		));
	}
}
