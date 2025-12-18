package org.codenbug.event.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.event.application.FindEventService;
import org.codenbug.event.application.ImageUploadService;
import org.codenbug.event.application.RegisterEventService;
import org.codenbug.event.application.UpdateEventService;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.global.UpdateEventRequest;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Event Command", description = "이벤트 관리 API (생성, 수정, 삭제)")
public class EventCommandController {

	private final RegisterEventService registerEventService;
	private final UpdateEventService updateEventService;
	private final FindEventService findEventService;
	private final ImageUploadService imageUploadService;

	public EventCommandController(RegisterEventService registerEventService, UpdateEventService updateEventService,
		FindEventService findEventService, ImageUploadService imageUploadService) {
		this.registerEventService = registerEventService;
		this.updateEventService = updateEventService;
		this.findEventService = findEventService;
		this.imageUploadService = imageUploadService;
	}

	@Operation(summary = "이벤트 등록", description = "새로운 이벤트를 등록합니다. 매니저 권한이 필요합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이벤트 등록 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족 (매니저 권한 필요)")
	})
	@AuthNeeded
	@RoleRequired(Role.MANAGER)
	@PostMapping
	public ResponseEntity<RsData<EventId>> eventRegister(
		@Parameter(description = "이벤트 등록 정보", required = true)
		@Valid @RequestBody NewEventRequest request) {

		// register event
		EventId eventId = registerEventService.registerNewEvent(request);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 등록 성공",
			eventId
		));
	}

	@Operation(summary = "이벤트 수정", description = "기존 이벤트 정보를 수정합니다. 매니저 또는 관리자 권한이 필요합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이벤트 수정 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족"),
		@ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
	})
	@AuthNeeded
	@RoleRequired({Role.MANAGER, Role.ADMIN})
	@PutMapping("/{eventId}")
	public ResponseEntity<RsData<EventId>> updateEvent(
		@Parameter(description = "수정할 이벤트 ID", required = true)
		@PathVariable String eventId,
		@Parameter(description = "이벤트 수정 정보", required = true)
		@RequestBody UpdateEventRequest request
	) {
		updateEventService.updateEvent(new EventId(eventId), request);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 수정 성공",
			new EventId(eventId)
		));
	}

	@Operation(summary = "이벤트 삭제", description = "이벤트를 삭제합니다. 매니저 또는 관리자 권한이 필요합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이벤트 삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족"),
		@ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
	})
	@AuthNeeded
	@RoleRequired({Role.MANAGER, Role.ADMIN})
	@DeleteMapping("/{eventId}")
	public ResponseEntity<RsData<Void>> deleteEvent(
		@Parameter(description = "삭제할 이벤트 ID", required = true)
		@PathVariable String eventId) {
		updateEventService.deleteEvent(new EventId(eventId));
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 삭제 성공",
			null
		));
	}



	@Operation(summary = "이벤트 상태 변경", description = "이벤트의 상태를 변경합니다. 매니저 또는 관리자 권한이 필요합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이벤트 상태 변경 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족"),
		@ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
	})
	@AuthNeeded
	@RoleRequired({Role.MANAGER, Role.ADMIN})
	@PatchMapping("/{eventId}")
	public ResponseEntity<RsData<EventId>> changeStatus(
		@Parameter(description = "상태를 변경할 이벤트 ID", required = true)
		@PathVariable String eventId, 
		@Parameter(description = "변경할 이벤트 상태", required = true)
		@RequestParam(name = "status") String status){
		updateEventService.changeStatus(eventId, status);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 상태 변경 성공",
			new EventId(eventId)
		));
	}


}
