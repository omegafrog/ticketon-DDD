package org.codenbug.purchase.app.es;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStreamRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmCommandServiceTest {
	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private PurchaseRepository purchaseRepository;
	@Mock
	private JpaPurchaseEventStreamRepository streamRepository;
	@Mock
	private JpaPurchaseEventStoreRepository eventStoreRepository;
	@Mock
	private JpaPurchaseOutboxRepository outboxRepository;
	@Mock
	private JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;

	@InjectMocks
	private PurchaseConfirmCommandService service;

	@Test
	void requestConfirm_whenAlreadyRequested_doesNotAppendOrEnqueue() {
		ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
		Purchase purchase = org.mockito.Mockito.mock(Purchase.class);

		when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
		when(purchase.getExpectedSalesVersion()).thenReturn(1L);
		when(eventStoreRepository.existsByPurchaseIdAndCommandId("p1", "confirm:p1")).thenReturn(true);

		service.requestConfirm(req, "u1");

		verify(eventStoreRepository, never()).save(any());
		verify(outboxRepository, never()).save(any());
		verify(streamRepository, never()).save(any());
	}

	@Test
	void requestConfirm_whenFirstRequested_appendsAndEnqueues() {
		ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
		Purchase purchase = org.mockito.Mockito.mock(Purchase.class);

		when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
		when(purchase.getExpectedSalesVersion()).thenReturn(1L);
		when(purchase.getEventId()).thenReturn("e1");
		when(eventStoreRepository.existsByPurchaseIdAndCommandId("p1", "confirm:p1")).thenReturn(false);
		when(streamRepository.findById("p1")).thenReturn(Optional.empty());
		when(streamRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(statusProjectionRepository.findById("p1")).thenReturn(Optional.empty());

		service.requestConfirm(req, "u1");

		verify(eventStoreRepository).save(any());
		verify(outboxRepository).save(any());
		verify(statusProjectionRepository).save(any());
	}
}
