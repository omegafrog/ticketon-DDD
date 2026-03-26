package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.Map;

import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.PaymentValidationService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.global.InitiatePaymentRequest;
import org.codenbug.purchase.global.InitiatePaymentResponse;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PurchaseInitCommandService {
	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;
	private final PaymentValidationService paymentValidationService;
	private final JpaPurchaseEventStoreRepository eventStoreRepository;

	public PurchaseInitCommandService(
		ObjectMapper objectMapper,
		PurchaseRepository purchaseRepository,
		PaymentValidationService paymentValidationService,
		JpaPurchaseEventStoreRepository eventStoreRepository
	) {
		this.objectMapper = objectMapper;
		this.purchaseRepository = purchaseRepository;
		this.paymentValidationService = paymentValidationService;
		this.eventStoreRepository = eventStoreRepository;
	}

	@Transactional
	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userId) {
		paymentValidationService.validatePaymentRequest(request.getEventId(), request.getAmount());
		EventSummary eventSummary = paymentValidationService.getEventSummary(request.getEventId());

		Purchase purchase = new Purchase(
			request.getEventId(),
			request.getOrderId(),
			request.getAmount(),
			eventSummary.getSalesVersion(),
			new UserId(userId)
		);

		purchaseRepository.save(purchase);

		String purchaseId = purchase.getPurchaseId().getValue();
		String commandId = "init:" + purchaseId;

		LocalDateTime now = LocalDateTime.now();
		String payloadJson = toJson(Map.of(
			"purchaseId", purchaseId,
			"userId", userId,
			"eventId", purchase.getEventId(),
			"orderId", purchase.getOrderId(),
			"amount", purchase.getAmount(),
			"expectedSalesVersion", purchase.getExpectedSalesVersion(),
			"status", purchase.getPaymentStatus().name()
		));
		String metadataJson = toJson(Map.of("commandId", commandId));
		eventStoreRepository.save(new PurchaseStoredEvent(
			purchaseId,
			PurchaseEventType.PAYMENT_INITIATED.name(),
			commandId,
			payloadJson,
			metadataJson,
			now
		));

		return new InitiatePaymentResponse(purchaseId, purchase.getPaymentStatus().name());
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new IllegalStateException("json serialization failed", e);
		}
	}
}
