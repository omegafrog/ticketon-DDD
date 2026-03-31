package org.codenbug.purchase.app.es;

import static org.codenbug.common.transaction.TransactionExecutor.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codenbug.purchase.app.PGApiService;
import org.codenbug.purchase.app.PaymentProvider;
import org.codenbug.purchase.app.PaymentProviderRouter;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseProcessedMessage;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;
import org.codenbug.purchase.infra.client.EventPaymentHoldClient;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseProcessedMessageRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PurchaseConfirmWorker {
	private final ObjectMapper objectMapper;
	private final PlatformTransactionManager transactionManager;
	private final JpaPurchaseProcessedMessageRepository processedMessageRepository;
	private final JpaPurchaseEventStoreRepository eventStoreRepository;
	private final PurchaseEventAppendService eventAppendService;
	private final EventPaymentHoldClient holdClient;
	private final PaymentProviderRouter paymentProviderRouter;
	private final PurchasePaymentFinalizationService finalizationService;


	public PurchaseConfirmWorker(ObjectMapper objectMapper,
			@Qualifier("primaryTransactionManager") PlatformTransactionManager transactionManager,
			JpaPurchaseProcessedMessageRepository processedMessageRepository,
			JpaPurchaseEventStoreRepository eventStoreRepository, PurchaseEventAppendService eventAppendService,
			EventPaymentHoldClient holdClient, PaymentProviderRouter paymentProviderRouter,
			PurchasePaymentFinalizationService finalizationService) {

		this.objectMapper = objectMapper;
		this.transactionManager = transactionManager;
		this.processedMessageRepository = processedMessageRepository;
		this.eventStoreRepository = eventStoreRepository;
		this.eventAppendService = eventAppendService;
		this.holdClient = holdClient;
		this.paymentProviderRouter = paymentProviderRouter;
		this.finalizationService = finalizationService;

	}

	public void process(String messageId, String payloadJson) {
		if (messageId == null || messageId.isBlank()) {
			return;
		}

		if (!tryMarkProcessed(messageId)) {
			return;
		}

		String purchaseId = extractPurchaseId(payloadJson);
		if (purchaseId == null) {
			return;
		}

		ConfirmContext ctx = loadConfirmContext(purchaseId);
		if (ctx == null) {
			executeInTransaction(transactionManager, () -> {
				eventAppendService.appendAndUpdateProjection(purchaseId, "confirm:" + purchaseId,
						PurchaseEventType.PG_CONFIRM_FAILED, Map.of("reason", "missing_confirm_requested"),
						PurchaseConfirmStatus.FAILED, "missing confirm request");
				return null;
			});
			return;
		}
		if (ctx.terminal) {
			return;
		}

		executeInTransaction(transactionManager, () -> {
			eventAppendService.appendAndUpdateProjection(purchaseId, ctx.commandId,
					PurchaseEventType.PROCESSING_STARTED, Map.of("purchaseId", purchaseId),
					PurchaseConfirmStatus.PROCESSING, "processing");
			return null;
		});

		try {
			holdClient.acquireLock(ctx.eventId, ctx.expectedSalesVersion);
		} catch (HttpClientErrorException ex) {
			if (EventPaymentHoldClient.isHoldRejected(ex)) {
				executeInTransaction(transactionManager, () -> {
					eventAppendService.appendAndUpdateProjection(purchaseId, ctx.commandId,
							PurchaseEventType.HOLD_REJECTED, Map.of("eventId", ctx.eventId),
							PurchaseConfirmStatus.REJECTED, "event changed");
					return null;
				});
				return;
			}
			throw ex;
		}

		executeInTransaction(transactionManager, () -> {
			eventAppendService.appendAndUpdateProjection(purchaseId, ctx.commandId,
					PurchaseEventType.PG_CONFIRM_REQUESTED,
					Map.of("provider", ctx.provider, "paymentKey", ctx.paymentKey), PurchaseConfirmStatus.PROCESSING,
					"pg confirm requested");
			return null;
		});

		try {
			PGApiService pgApiService = paymentProviderRouter.get(PaymentProvider.from(ctx.provider));
			ConfirmedPaymentInfo paymentInfo = pgApiService.confirmPayment(ctx.paymentKey, ctx.orderId, ctx.amount,
					ctx.commandId);
			executeInTransaction(transactionManager, () -> {
				eventAppendService.appendAndUpdateProjection(purchaseId, ctx.commandId,
						PurchaseEventType.PG_CONFIRM_SUCCEEDED,
						Map.of("paymentKey", paymentInfo.getPaymentKey(), "status", paymentInfo.getStatus()),
						PurchaseConfirmStatus.PROCESSING, "pg confirm succeeded");
				ConfirmPaymentInfoAndResponse ignored = finalizeInTransaction(purchaseId, ctx, paymentInfo);
				eventAppendService.appendAndUpdateProjection(purchaseId, ctx.commandId,
						PurchaseEventType.PAYMENT_COMPLETED, Map.of("purchaseId", purchaseId),
						PurchaseConfirmStatus.DONE, "done");
				return null;
			});
		} catch (Exception ex) {
			executeInTransaction(transactionManager, () -> {
				eventAppendService.appendAndUpdateProjection(purchaseId, ctx.commandId,
						PurchaseEventType.PG_CONFIRM_FAILED, Map.of("error", ex.getClass().getSimpleName()),
						PurchaseConfirmStatus.FAILED, "pg confirm failed");
				return null;
			});
			throw ex;
		}
	}

	private boolean tryMarkProcessed(String messageId) {
		try {
			processedMessageRepository.save(new PurchaseProcessedMessage(messageId, LocalDateTime.now()));
			return true;
		} catch (DataIntegrityViolationException e) {
			return false;
		}
	}

	private String extractPurchaseId(String body) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(body, Map.class);
			Object val = map.get("purchaseId");
			return val == null ? null : val.toString();
		} catch (Exception e) {
			return null;
		}
	}

	private ConfirmContext loadConfirmContext(String purchaseId) {
		List<PurchaseStoredEvent> events = eventStoreRepository.findByPurchaseIdOrderByIdAsc(purchaseId);
		if (events.isEmpty()) {
			return null;
		}

		boolean terminal = false;
		Map<String, Object> confirmPayload = null;
		for (PurchaseStoredEvent ev : events) {
			if (PurchaseEventType.PAYMENT_COMPLETED.name().equals(ev.getEventType())) {
				terminal = true;
			}
			if (PurchaseEventType.CONFIRM_REQUESTED.name().equals(ev.getEventType())) {
				confirmPayload = parseJson(ev.getPayloadJson());
			}
		}
		if (confirmPayload == null) {
			return null;
		}

		ConfirmContext ctx = new ConfirmContext();
		ctx.purchaseId = purchaseId;
		ctx.commandId = "confirm:" + purchaseId;
		ctx.userId = String.valueOf(confirmPayload.get("userId"));
		ctx.eventId = String.valueOf(confirmPayload.get("eventId"));
		ctx.expectedSalesVersion = Long.valueOf(String.valueOf(confirmPayload.get("expectedSalesVersion")));
		ctx.paymentKey = String.valueOf(confirmPayload.get("paymentKey"));
		ctx.orderId = String.valueOf(confirmPayload.get("orderId"));
		ctx.amount = Integer.valueOf(String.valueOf(confirmPayload.get("amount")));
		ctx.provider = String.valueOf(confirmPayload.getOrDefault("provider", "TOSS"));
		ctx.terminal = terminal;
		return ctx;
	}

	private Map<String, Object> parseJson(String json) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(json, Map.class);
			return map;
		} catch (Exception e) {
			return new HashMap<>();
		}
	}

	private ConfirmPaymentInfoAndResponse finalizeInTransaction(String purchaseId, ConfirmContext ctx,
			ConfirmedPaymentInfo paymentInfo) {
		finalizationService.finalizePayment(purchaseId, paymentInfo, ctx.userId);
		return new ConfirmPaymentInfoAndResponse();
	}

	private static class ConfirmPaymentInfoAndResponse {
	}

	private static class ConfirmContext {
		String purchaseId;
		String commandId;
		String userId;
		String eventId;
		Long expectedSalesVersion;
		String paymentKey;
		String orderId;
		Integer amount;
		String provider;
		boolean terminal;
	}
}
