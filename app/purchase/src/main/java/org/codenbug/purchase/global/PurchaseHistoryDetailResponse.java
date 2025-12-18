package org.codenbug.purchase.global;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.purchase.domain.Purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PurchaseHistoryDetailResponse {
	private List<PurchaseDto> purchases; // 구매 이력 목록 -> PurchaseDto를 통해 구매이력 객체를 리스트로 반환 의도

	@Getter
	@Builder
	@AllArgsConstructor
	public static class PurchaseDto {
		private String purchaseId;
		private String paymentKey;
		private String eventId;
		private String itemName;
		private Integer amount;
		private LocalDateTime purchaseDate;
		private String paymentMethod;
		private String paymentStatus;
		private List<TicketInfo> tickets;

		public static PurchaseDto of(Purchase purchase){
			return PurchaseHistoryDetailResponse.PurchaseDto.builder()
				.purchaseId(purchase.getPurchaseId().getValue())
				.paymentKey(purchase.getPid())
				.eventId(purchase.getEventId())
				.itemName(purchase.getOrderName())
				.amount(purchase.getAmount())
				.purchaseDate(purchase.getCreatedAt())
				.paymentMethod(purchase.getPaymentMethod().name())
				.paymentStatus(purchase.getPaymentStatus().name())
				.tickets(purchase.getTickets().stream()
					.map(ticket -> PurchaseHistoryDetailResponse.TicketInfo.builder()
						.ticketId(ticket.getId().getValue())
						.seatLocation(ticket.getLocation())
						.build())
					.toList())
				.build();
		}
	}

	@Getter
	@Builder
	@AllArgsConstructor
	public static class TicketInfo {
		private String ticketId;
		private String seatLocation;
	}
} 