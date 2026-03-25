package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.codenbug.purchase.domain.MessagePublisher;
import org.codenbug.purchase.domain.PaymentMethod;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseDomainService;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.TicketRepository;
import org.codenbug.purchase.global.ConfirmPaymentResponse;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.springframework.stereotype.Service;

@Service
public class PurchasePaymentFinalizationService {
	private final PurchaseRepository purchaseRepository;
	private final TicketRepository ticketRepository;
	private final PurchaseDomainService purchaseDomainService;
	private final MessagePublisher publisher;

	public PurchasePaymentFinalizationService(PurchaseRepository purchaseRepository, TicketRepository ticketRepository,
		PurchaseDomainService purchaseDomainService, MessagePublisher publisher) {
		this.purchaseRepository = purchaseRepository;
		this.ticketRepository = ticketRepository;
		this.purchaseDomainService = purchaseDomainService;
		this.publisher = publisher;
	}

	public ConfirmPaymentResponse finalizePayment(String purchaseId, ConfirmedPaymentInfo paymentInfo, String userId) {
		Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
			.orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));

		PurchaseDomainService.PurchaseConfirmationResult result =
			purchaseDomainService.confirmPurchase(purchase, paymentInfo, userId);

		purchase.markAsCompleted();
		ticketRepository.saveAll(result.getTickets());
		purchaseRepository.save(purchase);

		publisher.publishSeatPurchasedEvent(
			purchase.getEventId(),
			result.getSeatLayout().getLayoutId(),
			result.getSeatIds(),
			userId
		);

		PaymentMethod methodEnum = PaymentMethod.from(paymentInfo.getMethod());
		LocalDateTime localDateTime = OffsetDateTime.parse(paymentInfo.getApprovedAt())
			.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
			.toLocalDateTime();

		return new ConfirmPaymentResponse(
			paymentInfo.getPaymentKey(),
			paymentInfo.getOrderId(),
			paymentInfo.getOrderName(),
			paymentInfo.getTotalAmount(),
			paymentInfo.getStatus(),
			methodEnum,
			localDateTime,
			new ConfirmPaymentResponse.Receipt(paymentInfo.getReceipt().getUrl())
		);
	}
}
