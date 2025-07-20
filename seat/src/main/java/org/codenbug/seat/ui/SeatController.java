package org.codenbug.seat.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.common.redis.EntryTokenValidator;
import org.codenbug.seat.app.FindSeatLayoutService;
import org.codenbug.seat.app.UpdateSeatLayoutService;
import org.codenbug.seat.global.SeatCancelRequest;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.codenbug.seat.global.SeatSelectRequest;
import org.codenbug.seat.global.SeatSelectResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class SeatController {
	private final FindSeatLayoutService findSeatLayoutService;
	private final UpdateSeatLayoutService updateSeatLayoutService;
	private final EntryTokenValidator entryTokenValidator;
	/**
	 * 좌석 조회 API
	 *
	 * @param eventId 조회할 이벤트 ID
	 * @return 좌석 선택 결과 응답
	 */
	@GetMapping("/{event-id}/seats")
	public ResponseEntity<RsData<SeatLayoutResponse>> getSeatLayout(@PathVariable("event-id") String eventId) {
		SeatLayoutResponse seatLayoutResponse = findSeatLayoutService.findSeatLayoutByEventId(eventId);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"좌석 조회 성공",
			seatLayoutResponse
		));
	}

	/**
	 * 좌석 선택 API
	 *
	 * @param eventId           조회할 이벤트 ID
	 * @param seatSelectRequest 사용자가 선택한 좌석 ID 목록
	 * @return 좌석 선택 결과 응답
	 */
	@PostMapping("/{event-id}/seats")
	@AuthNeeded
	@RoleRequired({Role.USER})
	public ResponseEntity<RsData<SeatSelectResponse>> selectSeat(
		@PathVariable("event-id") String eventId,
		@RequestBody SeatSelectRequest seatSelectRequest,
		@RequestHeader("entryAuthToken") String entryAuthToken) {
		entryTokenValidator.validate(LoggedInUserContext.get().getUserId(), entryAuthToken);

		SeatSelectResponse seatSelectResponse = updateSeatLayoutService.selectSeat(eventId, seatSelectRequest, LoggedInUserContext.get().getUserId());
		return ResponseEntity.ok(new RsData<>(
			"200",
			"좌석 선택 성공",
			seatSelectResponse
		));
	}

	/**
	 * 좌석 취소 API
	 *
	 * @param eventId           조회할 이벤트 ID
	 * @param seatCancelRequest 사용자가 취소한 좌석 ID 목록
	 * @return 좌석 취소 결과 응답
	 */
	@DeleteMapping("/{event-id}/seats")
	public ResponseEntity<RsData<Void>> cancelSeat(@PathVariable("event-id") String eventId,
		@RequestBody SeatCancelRequest seatCancelRequest,
		@RequestHeader("entryAuthToken") String entryAuthToken) {
		String userId = LoggedInUserContext.get().getUserId();

		entryTokenValidator.validate(userId, entryAuthToken);
		updateSeatLayoutService.cancelSeat(eventId, seatCancelRequest, userId);
		return ResponseEntity.ok(new RsData<>(
			"200",
			"좌석 취소 성공",
			null
		));
	}
}


