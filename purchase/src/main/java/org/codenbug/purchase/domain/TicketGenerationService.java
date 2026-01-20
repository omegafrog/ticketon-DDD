package org.codenbug.purchase.domain;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 티켓 생성을 담당하는 도메인 서비스
 */
@Component
@RequiredArgsConstructor
public class TicketGenerationService {

	/**
	 * 구매한 좌석들에 대한 티켓을 생성합니다.
	 */
	public List<Ticket> generateTickets(
		Purchase purchase, 
		List<String> seatIds, 
		EventSummary eventSummary,
		SeatLayoutInfo seatLayout
	) {
		List<SeatInfo> seats = seatLayout.getSeats().stream()
			.filter(seat -> seatIds.contains(seat.getSeatId()))
			.toList();

		return seats.stream()
			.map(seat -> new Ticket(
				seatLayout.getLocationName(), 
				seat.getSeatId(), 
				purchase
			))
			.toList();
	}

	/**
	 * 구매 이름을 생성합니다.
	 */
	public String generateOrderName(EventSummary eventSummary, int seatCount) {
		return eventSummary.isSeatSelectable() 
			? "지정석 %d매".formatted(seatCount)
			: "미지정석 %d매".formatted(seatCount);
	}
}
