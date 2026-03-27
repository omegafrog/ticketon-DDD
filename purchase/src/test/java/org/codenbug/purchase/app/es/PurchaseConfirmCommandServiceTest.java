package org.codenbug.purchase.app.es;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
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
		when(eventStoreRepository.existsByPurchaseIdAndCommandId("p1", "confirm:p1")).thenReturn(true);

		service.requestConfirm(req, "u1");

		verify(eventStoreRepository, never()).save(any());
		verify(outboxRepository, never()).save(any());
	}

	@Test
	void requestConfirm_whenFirstRequested_appendsAndEnqueues() {
		ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
		Purchase purchase = org.mockito.Mockito.mock(Purchase.class);

		when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
		when(purchase.getEventId()).thenReturn("e1");
		when(eventStoreRepository.existsByPurchaseIdAndCommandId("p1", "confirm:p1")).thenReturn(false);
		when(eventStoreRepository.findByPurchaseIdOrderByIdAsc("p1")).thenReturn(List.of(
			new PurchaseStoredEvent(
				"p1",
				PurchaseEventType.PAYMENT_INITIATED.name(),
				"init:p1",
				"{\"expectedSalesVersion\":1}",
				"{}",
				LocalDateTime.now()
			)
		));
		when(eventStoreRepository.save(any())).thenAnswer(invocation -> {
			Object arg = invocation.getArgument(0);
			if (arg != null) {
				try {
					Field idField = arg.getClass().getDeclaredField("id");
					idField.setAccessible(true);
					idField.set(arg, 1L);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			}
			return arg;
		});
		when(statusProjectionRepository.findById("p1")).thenReturn(Optional.empty());

		service.requestConfirm(req, "u1");

		verify(eventStoreRepository).save(any());
		verify(outboxRepository).save(any());
		verify(statusProjectionRepository).save(any());
	}

	@Test
	void requestConfirm_whenInitEventMissingAndPurchaseVersionMissing_throws() {
		ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
		Purchase purchase = org.mockito.Mockito.mock(Purchase.class);

		when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
		when(purchase.getExpectedSalesVersion()).thenReturn(null);
		when(eventStoreRepository.existsByPurchaseIdAndCommandId("p1", "confirm:p1")).thenReturn(false);
		when(eventStoreRepository.findByPurchaseIdOrderByIdAsc("p1")).thenReturn(List.of());

		org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
			() -> service.requestConfirm(req, "u1"));

		verify(eventStoreRepository, never()).save(any());
		verify(outboxRepository, never()).save(any());
	}
}
