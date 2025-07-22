package org.codenbug.purchase.domain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.codenbug.common.redis.RedisLockService;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;
import org.codenbug.purchase.query.model.EventProjection;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Purchase 애그리게이트 관련 도메인 로직을 처리하는 도메인 서비스
 */
@Component
@RequiredArgsConstructor
public class PurchaseDomainService {
	private final PaymentValidationService paymentValidationService;
	private final TicketGenerationService ticketGenerationService;
	private final RedisLockService redisLockService;

	/**
	 * 결제 승인 처리의 핵심 도메인 로직
	 */
	public PurchaseConfirmationResult confirmPurchase(
		Purchase purchase, 
		ConfirmedPaymentInfo paymentInfo,
		String userId
	) {
		// 1. 좌석 정보 조회 및 검증
		List<String> seatIds = redisLockService.getLockedSeatIdsByUserId(userId);
		EventProjection eventProjection = paymentValidationService.getEventProjection(purchase.getEventId());
		paymentValidationService.validateSeatSelection(purchase.getEventId(), seatIds);

		// 2. 좌석 레이아웃 조회
		SeatLayoutProjection seatLayout = paymentValidationService.getSeatLayoutProjection(
			eventProjection.getSeatLayoutId());

		// 3. 티켓 생성
		List<Ticket> tickets = ticketGenerationService.generateTickets(
			purchase, seatIds, eventProjection, seatLayout);

		// 4. 결제 정보 업데이트
		PaymentMethod methodEnum = PaymentMethod.from(paymentInfo.getMethod());
		LocalDateTime approvedAt = OffsetDateTime.parse(paymentInfo.getApprovedAt())
			.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
			.toLocalDateTime();

		String orderName = ticketGenerationService.generateOrderName(eventProjection, seatIds.size());

		purchase.updatePaymentInfo(
			paymentInfo.getPaymentKey(),
			paymentInfo.getOrderId(),
			paymentInfo.getTotalAmount(),
			methodEnum,
			orderName,
			approvedAt
		);

		return new PurchaseConfirmationResult(tickets, seatLayout, seatIds);
	}

	/**
	 * 결제 승인 결과를 담는 값 객체
	 */
	public static class PurchaseConfirmationResult {
		private final List<Ticket> tickets;
		private final SeatLayoutProjection seatLayout;
		private final List<String> seatIds;

		public PurchaseConfirmationResult(List<Ticket> tickets, SeatLayoutProjection seatLayout, List<String> seatIds) {
			this.tickets = tickets;
			this.seatLayout = seatLayout;
			this.seatIds = seatIds;
		}

		public List<Ticket> getTickets() {
			return tickets;
		}

		public SeatLayoutProjection getSeatLayout() {
			return seatLayout;
		}

		public List<String> getSeatIds() {
			return seatIds;
		}
	}
}