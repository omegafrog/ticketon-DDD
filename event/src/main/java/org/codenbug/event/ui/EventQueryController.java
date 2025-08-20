package org.codenbug.event.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.event.application.EventQueryService;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.EventListFilter;
import org.codenbug.event.global.EventListResponse;
import org.codenbug.event.global.EventManagerListResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventQueryController {
	private final EventQueryService eventQueryService;

	public EventQueryController(EventQueryService service){
		this.eventQueryService = service;
	}
	@PostMapping("/list")
	public ResponseEntity<RsData<Page<EventListResponse>>> getEvents(
		@RequestParam(name = "keyword", required = false) String keyword,
		 @RequestBody(required = false) EventListFilter filter, Pageable pageable) {
		Page<EventListResponse> eventList = eventQueryService.getEvents(keyword, filter, pageable);

		return ResponseEntity.ok(new RsData("200","event list 조회 성공.", eventList));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RsData<EventInfoResponse>> getEvent(@PathVariable(name = "id") String id) {
		EventInfoResponse event = eventQueryService.getEvent(id);

		return ResponseEntity.ok(new RsData("200", "event 단건 조회 성공.", event));
	}

	@GetMapping("/manager/me")
	@RoleRequired(Role.MANAGER)
	@AuthNeeded
	public ResponseEntity<RsData<Page<EventManagerListResponse>>> getManagerEvents(Pageable pageable) {
		String userId = LoggedInUserContext.get().getUserId();
		ManagerId managerId = new ManagerId(userId);
		Page<EventManagerListResponse> events = eventQueryService.getManagerEvents(managerId, pageable);
		
		return ResponseEntity.ok(new RsData("200", "매니저 이벤트 리스트 조회 성공.", events));
	}

}
