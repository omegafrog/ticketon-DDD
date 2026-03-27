package org.codenbug.seat.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.seat.app.FindSeatLayoutService;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Seat Query", description = "좌석 조회 API")
public class SeatQueryController {

	private final FindSeatLayoutService findSeatLayoutService;

	@Operation(summary = "좌석 조회", description = "id가 eventId인 이벤트의 좌석을 조회")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 정보 필요")
	})
	@AuthNeeded
	@RoleRequired(value = {Role.USER})
	@GetMapping("/{event-id}/seats")
	public ResponseEntity<RsData<SeatLayoutResponse>> getSeatLayout(@PathVariable("event-id") String eventId) {
		SeatLayoutResponse seatLayoutResponse = findSeatLayoutService.findSeatLayoutByEventId(eventId);
		return ResponseEntity.ok(new RsData<>("200", "좌석 조회 성공", seatLayoutResponse));
	}
}
