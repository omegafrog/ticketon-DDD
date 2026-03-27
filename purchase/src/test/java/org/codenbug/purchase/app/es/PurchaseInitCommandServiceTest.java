package org.codenbug.purchase.app.es;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.PaymentValidationService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.global.InitiatePaymentRequest;
import org.codenbug.purchase.global.InitiatePaymentResponse;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseInitCommandServiceTest {
	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private PurchaseRepository purchaseRepository;
	@Mock
	private PaymentValidationService paymentValidationService;
	@Mock
	private JpaPurchaseEventStoreRepository eventStoreRepository;

	@InjectMocks
	private PurchaseInitCommandService service;

	@Test
	void initiatePayment_savesPurchaseAndAppendsPaymentInitiatedEvent() {
		InitiatePaymentRequest request = new InitiatePaymentRequest("event-1", "order-1", 12000);
		EventSummary summary = new EventSummary("event-1", 10L, true, "OPEN", 3L, 7L, "title");

		when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(paymentValidationService.getEventSummary("event-1")).thenReturn(summary);

		InitiatePaymentResponse response = service.initiatePayment(request, "user-1");

		ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
		verify(purchaseRepository).save(purchaseCaptor.capture());
		Purchase savedPurchase = purchaseCaptor.getValue();

		ArgumentCaptor<PurchaseStoredEvent> eventCaptor = ArgumentCaptor.forClass(PurchaseStoredEvent.class);
		verify(eventStoreRepository).save(eventCaptor.capture());
		PurchaseStoredEvent savedEvent = eventCaptor.getValue();

		assertEquals(savedPurchase.getPurchaseId().getValue(), response.getPurchaseId());
		assertEquals(savedPurchase.getPaymentStatus().name(), response.getStatus());
		assertEquals(PurchaseEventType.PAYMENT_INITIATED.name(), savedEvent.getEventType());
		assertEquals(savedPurchase.getPurchaseId().getValue(), savedEvent.getPurchaseId());
		assertEquals("init:" + savedPurchase.getPurchaseId().getValue(), savedEvent.getCommandId());
		verify(paymentValidationService).validatePaymentRequest("event-1", 12000);
	}
}
