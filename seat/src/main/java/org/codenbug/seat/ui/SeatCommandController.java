package org.codenbug.seat.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.common.redis.EntryTokenValidator;
import org.codenbug.seat.app.UpdateSeatLayoutService;
import org.codenbug.seat.global.SeatCancelRequest;
import org.codenbug.seat.global.SeatSelectRequest;
import org.codenbug.seat.global.SeatSelectResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Seat Command", description = "좌석 변경 API")
public class SeatCommandController {

	private final UpdateSeatLayoutService updateSeatLayoutService;
	private final EntryTokenValidator entryTokenValidator;

	@Operation(summary = "좌석 선택", description = "id가 eventId인 이벤트의 좌석 id 리스트에 해당하는 좌석을 선택")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 선택 성공"),
		@ApiResponse(responseCode = "401", description = "인증 정보 필요")
	})
	@PostMapping("/{event-id}/seats")
	@AuthNeeded
	@RoleRequired({Role.USER})
	public ResponseEntity<RsData<SeatSelectResponse>> selectSeat(
			@PathVariable("event-id") String eventId,
			@Valid @RequestBody SeatSelectRequest seatSelectRequest,
			@RequestHeader("entryAuthToken") String entryAuthToken) {
		entryTokenValidator.validate(LoggedInUserContext.get().getUserId(), entryAuthToken, eventId);
		SeatSelectResponse seatSelectResponse = updateSeatLayoutService.selectSeat(
			eventId,
			seatSelectRequest,
			LoggedInUserContext.get().getUserId()
		);
		return ResponseEntity.ok(new RsData<>("200", "좌석 선택 성공", seatSelectResponse));
	}

	@Operation(summary = "좌석 선택 취소", description = "id가 eventId인 이벤트의 좌석 id 리스트에 해당하는 좌석 선택을 취소")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 선택 취소"),
		@ApiResponse(responseCode = "401", description = "인증 정보 필요")
	})
	@DeleteMapping("/{event-id}/seats")
	public ResponseEntity<RsData<Void>> cancelSeat(
			@PathVariable("event-id") String eventId,
			@Valid @RequestBody SeatCancelRequest seatCancelRequest,
			@RequestHeader("entryAuthToken") String entryAuthToken) {
		String userId = LoggedInUserContext.get().getUserId();
		entryTokenValidator.validate(userId, entryAuthToken, eventId);
		updateSeatLayoutService.cancelSeat(eventId, seatCancelRequest, userId);
		return ResponseEntity.ok(new RsData<>("200", "좌석 취소 성공", null));
	}
}
