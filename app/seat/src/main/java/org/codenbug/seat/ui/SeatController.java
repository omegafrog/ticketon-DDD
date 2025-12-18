package org.codenbug.seat.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Seat", description = "좌석 관리 API")
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
	@Operation(summary = "좌석 조회", description = "id가 eventId인 이벤트의 좌석을 조회")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 정보 필요")})
	@AuthNeeded
	@RoleRequired(value = {Role.USER})
	@GetMapping("/{event-id}/seats")
	public ResponseEntity<RsData<SeatLayoutResponse>> getSeatLayout(
			@PathVariable("event-id") String eventId) {
		SeatLayoutResponse seatLayoutResponse = findSeatLayoutService.findSeatLayoutByEventId(eventId);
		return ResponseEntity.ok(new RsData<>("200", "좌석 조회 성공", seatLayoutResponse));
	}

	/**
	 * 좌석 선택 API
	 *
	 * @param eventId 조회할 이벤트 ID
	 * @param seatSelectRequest 사용자가 선택한 좌석 ID 목록
	 * @return 좌석 선택 결과 응답
	 */
	@Operation(summary = "좌석 선택",
			description = "id가 eventId인 이벤트의 seatSelectRequest의 좌석 id 리스트에 해당하는 좌석을 선택")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "좌석 선택 성공"),
			@ApiResponse(responseCode = "401", description = "인증 정보 필요")})
	@PostMapping("/{event-id}/seats")
	@AuthNeeded
	@RoleRequired({Role.USER})
	public ResponseEntity<RsData<SeatSelectResponse>> selectSeat(
			@PathVariable("event-id") String eventId, @RequestBody SeatSelectRequest seatSelectRequest,
			@RequestHeader("entryAuthToken") String entryAuthToken) {
		entryTokenValidator.validate(LoggedInUserContext.get().getUserId(), entryAuthToken);

		SeatSelectResponse seatSelectResponse = updateSeatLayoutService.selectSeat(eventId,
				seatSelectRequest, LoggedInUserContext.get().getUserId());
		return ResponseEntity.ok(new RsData<>("200", "좌석 선택 성공", seatSelectResponse));
	}

	/**
	 * 좌석 취소 API
	 *
	 * @param eventId 조회할 이벤트 ID
	 * @param seatCancelRequest 사용자가 취소한 좌석 ID 목록
	 * @return 좌석 취소 결과 응답
	 */
	@Operation(summary = "좌석 선택 취소",
			description = "id가 eventId인 이벤트의 seatCancelRequest의 좌석 id 리스트에 해당하는 좌석 선택을 취소")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "좌석 선택 취소"),
			@ApiResponse(responseCode = "401", description = "인증 정보 필요")})
	@DeleteMapping("/{event-id}/seats")
	public ResponseEntity<RsData<Void>> cancelSeat(@PathVariable("event-id") String eventId,
			@RequestBody SeatCancelRequest seatCancelRequest,
			@RequestHeader("entryAuthToken") String entryAuthToken) {
		String userId = LoggedInUserContext.get().getUserId();

		entryTokenValidator.validate(userId, entryAuthToken);
		updateSeatLayoutService.cancelSeat(eventId, seatCancelRequest, userId);
		return ResponseEntity.ok(new RsData<>("200", "좌석 취소 성공", null));
	}
}


