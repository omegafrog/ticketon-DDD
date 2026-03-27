package org.codenbug.purchase.app.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmSchedulerTest {

	@Mock
	private JpaPurchaseOutboxRepository outboxRepository;

	@Mock
	private JpaPurchaseConfirmStatusProjectionRepository statusProjectionRepository;

	@Mock
	private PurchaseEventAppendService eventAppendService;

	@Mock
	private PurchaseConfirmWorker confirmWorker;

	private PurchaseConfirmScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new PurchaseConfirmScheduler(
			new ObjectMapper(),
			outboxRepository,
			statusProjectionRepository,
			eventAppendService,
			confirmWorker,
			3
		);
	}

	@ParameterizedTest
	@EnumSource(value = PurchaseConfirmStatus.class, names = {"PENDING", "PROCESSING"})
	void processPendingConfirms_retriesForRetryableStatuses(PurchaseConfirmStatus status) {
		PurchaseOutboxMessage message = outbox("purchase-retryable");
		when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
			.thenReturn(List.of(message));
		when(statusProjectionRepository.findById("purchase-retryable"))
			.thenReturn(Optional.of(projection("purchase-retryable", status)));

		scheduler.processPendingConfirms();

		verify(confirmWorker, times(1)).process(eq(message.getMessageId()), eq(message.getPayloadJson()));
		verify(outboxRepository, atLeastOnce()).save(eq(message));
		assertThat(message.getPublishedAt()).isNotNull();
		assertThat(message.getLastError()).isNull();
		verifyNoInteractions(eventAppendService);
	}

	@ParameterizedTest
	@EnumSource(value = PurchaseConfirmStatus.class, names = {"DONE", "FAILED", "REJECTED"})
	void processPendingConfirms_skipsTerminalStatuses(PurchaseConfirmStatus terminalStatus) {
		PurchaseOutboxMessage message = outbox("purchase-terminal");
		when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
			.thenReturn(List.of(message));
		when(statusProjectionRepository.findById("purchase-terminal"))
			.thenReturn(Optional.of(projection("purchase-terminal", terminalStatus)));

		scheduler.processPendingConfirms();

		verify(confirmWorker, never()).process(anyString(), anyString());
		verify(outboxRepository, atLeastOnce()).save(eq(message));
		assertThat(message.getPublishedAt()).isNotNull();
		assertThat(message.getLastError()).contains("non-retryable status");
		verifyNoInteractions(eventAppendService);
	}

	@Test
	void processPendingConfirms_marksFailedWhenMaxAttemptsExceeded() {
		PurchaseOutboxMessage message = outbox("purchase-max-attempt");
		message.markPublishAttemptFailed("e1");
		message.markPublishAttemptFailed("e2");
		message.markPublishAttemptFailed("e3");

		when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
			.thenReturn(List.of(message));
		when(statusProjectionRepository.findById("purchase-max-attempt"))
			.thenReturn(Optional.of(projection("purchase-max-attempt", PurchaseConfirmStatus.PENDING)));

		scheduler.processPendingConfirms();

		verify(confirmWorker, never()).process(anyString(), anyString());
		verify(eventAppendService, times(1)).appendAndUpdateProjection(
			eq("purchase-max-attempt"),
			eq("confirm:purchase-max-attempt"),
			eq(PurchaseEventType.PG_CONFIRM_FAILED),
			anyMap(),
			eq(PurchaseConfirmStatus.FAILED),
			eq("max attempts exceeded")
		);
		assertThat(message.getPublishedAt()).isNotNull();
		assertThat(message.getLastError()).contains("max publish attempts exceeded");
	}

	@Test
	void processPendingConfirms_keepsOutboxUnpublishedWhenWorkerThrows() {
		PurchaseOutboxMessage message = outbox("purchase-throw");
		when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
			.thenReturn(List.of(message));
		when(statusProjectionRepository.findById("purchase-throw"))
			.thenReturn(Optional.of(projection("purchase-throw", PurchaseConfirmStatus.PENDING)));
		doThrow(new IllegalStateException("boom")).when(confirmWorker).process(anyString(), anyString());

		scheduler.processPendingConfirms();

		verify(confirmWorker, times(1)).process(eq(message.getMessageId()), eq(message.getPayloadJson()));
		assertThat(message.getPublishedAt()).isNull();
		assertThat(message.getPublishAttempts()).isEqualTo(1);
		assertThat(message.getLastError()).contains("boom");
		verifyNoInteractions(eventAppendService);
	}

	private PurchaseOutboxMessage outbox(String purchaseId) {
		return PurchaseOutboxMessage.of(
			"msg-" + purchaseId,
			PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE,
			"{\"purchaseId\":\"" + purchaseId + "\"}",
			LocalDateTime.now()
		);
	}

	private PurchaseConfirmStatusProjection projection(String purchaseId, PurchaseConfirmStatus status) {
		return new PurchaseConfirmStatusProjection(
			purchaseId,
			status,
			1L,
			PurchaseEventType.CONFIRM_REQUESTED.name(),
			"test",
			LocalDateTime.now()
		);
	}
}
