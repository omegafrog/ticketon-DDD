package org.codenbug.purchase.domain;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 결제 검증을 담당하는 도메인 서비스
 */
@Component
@RequiredArgsConstructor
public class PaymentValidationService {
	private final EventInfoProvider eventInfoProvider;
	private final SeatLayoutProvider seatLayoutProvider;

	/**
	 * 결제 요청의 기본 유효성을 검증합니다.
	 */
	public void validatePaymentRequest(String eventId, int amount) {
		eventInfoProvider.getEventSummary(eventId);

		if (amount <= 0) {
			throw new IllegalArgumentException("결제 금액이 잘못되었습니다.");
		}
	}

	/**
	 * 좌석 선택이 유효한지 검증합니다.
	 */
	public void validateSeatSelection(String eventId, List<String> seatIds) {
		EventSummary eventSummary = eventInfoProvider.getEventSummary(eventId);
		SeatLayoutInfo seatLayout = seatLayoutProvider.getSeatLayout(eventSummary.getSeatLayoutId());
		List<SeatInfo> seats = seatLayout.getSeats().stream().toList();
		
		for (String seatId : seatIds) {
			if (seats.stream().noneMatch(seat -> seat.getSeatId().equals(seatId))) {
				throw new IllegalArgumentException("존재하지 않는 좌석을 선택했습니다.");
			}
		}
	}

	/**
	 * 이벤트 정보를 조회합니다.
	 */
	public EventSummary getEventSummary(String eventId) {
		return eventInfoProvider.getEventSummary(eventId);
	}

	/**
	 * 좌석 레이아웃 정보를 조회합니다.
	 */
	public SeatLayoutInfo getSeatLayout(Long seatLayoutId) {
		return seatLayoutProvider.getSeatLayout(seatLayoutId);
	}
}
