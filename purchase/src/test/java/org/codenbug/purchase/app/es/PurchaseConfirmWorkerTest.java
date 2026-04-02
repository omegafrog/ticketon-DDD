package org.codenbug.purchase.app.es;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.purchase.app.PaymentProviderRouter;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.infra.client.EventServiceClient;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmWorkerTest {

	@Mock
	private PlatformTransactionManager transactionManager;

	@Mock
	private TransactionStatus transactionStatus;

	@Mock
	private JpaPurchaseProcessedMessageRepository processedMessageRepository;

	@Mock
	private JpaPurchaseEventStoreRepository eventStoreRepository;

	@Mock
	private PurchaseEventAppendService eventAppendService;

	@Mock
	private EventServiceClient eventServiceClient;

	@Mock
	private PaymentProviderRouter paymentProviderRouter;

	@Mock
	private PurchasePaymentFinalizationService finalizationService;

	private PurchaseConfirmWorker worker;

	@BeforeEach
	void setUp() {
		when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
		worker = new PurchaseConfirmWorker(
			new ObjectMapper(),
			transactionManager,
			processedMessageRepository,
			eventStoreRepository,
			eventAppendService,
			eventServiceClient,
			paymentProviderRouter,
			finalizationService
		);
	}

	@Test
	void process_whenEventVersionChanges_throwsPolicyViolation() {
		when(eventStoreRepository.findByPurchaseIdOrderByIdAsc("p1")).thenReturn(List.of(
			new PurchaseStoredEvent(
				"p1",
				PurchaseEventType.CONFIRM_REQUESTED.name(),
				"confirm:p1",
				"""
					{"userId":"u1","eventId":"e1","expectedSalesVersion":1,"paymentKey":"payKey","orderId":"order1","amount":1000,"provider":"TOSS"}
					""".trim(),
				"{}",
				LocalDateTime.now()
			)
		));
		when(eventServiceClient.getEventSummary("e1")).thenReturn(
			new EventSummary("e1", 1L, true, "OPEN", 2L, 2L, "event-title")
		);

		assertThatThrownBy(() -> worker.process("msg-1", "{\"purchaseId\":\"p1\"}"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("결제 도중 상품 내용이 변경되었습니다.");

		verify(eventAppendService).appendAndUpdateProjection(
			eq("p1"),
			eq("confirm:p1"),
			eq(PurchaseEventType.PROCESSING_STARTED),
			anyMap(),
			eq(PurchaseConfirmStatus.PROCESSING),
			eq("processing")
		);
		verify(eventAppendService, never()).appendAndUpdateProjection(
			eq("p1"),
			eq("confirm:p1"),
			eq(PurchaseEventType.PG_CONFIRM_REQUESTED),
			anyMap(),
			any(),
			anyString()
		);
		verifyNoInteractions(paymentProviderRouter, finalizationService);
	}
}
