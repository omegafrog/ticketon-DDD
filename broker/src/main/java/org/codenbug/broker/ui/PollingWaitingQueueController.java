package org.codenbug.broker.ui;

import org.codenbug.broker.app.PollingWaitingQueueService;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/broker/polling")
public class PollingWaitingQueueController {

	private final PollingWaitingQueueService service;

	public PollingWaitingQueueController(PollingWaitingQueueService service) {
		this.service = service;
	}

	@RoleRequired(Role.USER)
	@AuthNeeded
	@GetMapping("/events/{id}/waiting")
	public ResponseEntity<RsData<Void>> enterWaiting(@PathVariable(name = "id") String eventId) {
		service.enter(eventId);
		return new ResponseEntity<>(new RsData<>(
			HttpStatus.OK.toString(), "대기열 진입 성공.", null
		), HttpStatus.OK);
	}

	@RoleRequired(Role.USER)
	@AuthNeeded
	@GetMapping("/events/{id}/current")
	public ResponseEntity<RsData<PollingQueueInfo>> parseWaitingOrder(@PathVariable(name = "id")String eventId){
		PollingQueueInfo pollingQueueInfo = service.parseOrder(eventId);
		return new ResponseEntity<>(new RsData<>(HttpStatus.OK.toString(), "대기열 순번 조회 성공", pollingQueueInfo), HttpStatus.OK);
	}

	@RoleRequired(Role.USER)
	@AuthNeeded
	@DeleteMapping("/events/{id}/waiting")
	public ResponseEntity<RsData<Void>> disconnectWaiting(@PathVariable(name = "id") String eventId) {
		service.disconnect(eventId);
		return new ResponseEntity<>(new RsData<>(HttpStatus.OK.toString(), "대기열 탈출 성공", null), HttpStatus.OK);
	}
}
