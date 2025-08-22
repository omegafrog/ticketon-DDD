package org.codenbug.event.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.event.application.EventQueryService;
import org.codenbug.event.global.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.event.application.EventViewCountService;
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
	private final EventViewRepository eventViewRepository;
	private final EventViewCountService eventViewCountService;

	public EventQueryController(EventQueryService service, EventViewRepository eventViewRepository, 
	                           EventViewCountService eventViewCountService){
		this.eventQueryService = service;
		this.eventViewRepository = eventViewRepository;
		this.eventViewCountService = eventViewCountService;
	}
	@PostMapping("/list")
	public ResponseEntity<RsData<Page<EventListProjection>>> getEvents(
		@RequestParam(name = "keyword", required = false) String keyword,
		 @RequestBody(required = false) EventListFilter filter, Pageable pageable){
		// 최적화된 Projection 조회로 N+1 문제 해결 (Redis viewCount 포함)
		Page<EventListProjection> eventList = eventViewRepository.findEventList(keyword, filter, pageable);

		return ResponseEntity.ok(new RsData("200","event list 조회 성공.", eventList));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RsData<EventListProjection>> getEvent(@PathVariable(name = "id") String id) {
		// 최적화된 Projection으로 단건 조회 (Redis viewCount 포함)
		EventListProjection event = eventViewRepository.findEventById(id);
		
		// 비동기로 조회수 증가 (응답 속도에 영향 없음)
		eventViewCountService.incrementViewCountAsync(id);

		return ResponseEntity.ok(new RsData("200", "event 단건 조회 성공.", event));
	}

	@GetMapping("/manager/me")
	@RoleRequired(Role.MANAGER)
	@AuthNeeded
	public ResponseEntity<RsData<Page<EventListProjection>>> getManagerEvents(Pageable pageable) {
		String userId = LoggedInUserContext.get().getUserId();
		// 최적화된 Projection 조회로 N+1 문제 해결 (Redis viewCount 포함)
		Page<EventListProjection> events = eventViewRepository.findManagerEventList(userId, pageable);
		
		return ResponseEntity.ok(new RsData("200", "매니저 이벤트 리스트 조회 성공.", events));
	}

}
