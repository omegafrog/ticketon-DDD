package org.codenbug.seat.ui;

import org.codenbug.seat.app.FindSeatLayoutService;
import org.codenbug.common.RsData;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/seat-layouts")
@RequiredArgsConstructor
public class SeatInternalController {
	private final FindSeatLayoutService findSeatLayoutService;

	@GetMapping("/{layout-id}")
	public ResponseEntity<RsData<SeatLayoutResponse>> getSeatLayout(@PathVariable("layout-id") Long layoutId) {
		return ResponseEntity.ok(new RsData<>("200", "좌석 레이아웃 조회 성공",
				findSeatLayoutService.findSeatLayout(layoutId)));
	}
}
