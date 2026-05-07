package org.codenbug.purchase.app.command.es;

import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.port.es.PurchaseProcessedMessageStore;
import static org.codenbug.common.transaction.TransactionExecutor.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.codenbug.purchase.domain.port.PGApiService;
import org.codenbug.purchase.domain.PaymentProvider;
import org.codenbug.purchase.app.support.PaymentProviderRouter;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.EventInfoProvider;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseProcessedMessage;
import org.codenbug.purchase.domain.PaymentConfirmationInfo;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PurchaseConfirmWorker {
  private final ObjectMapper objectMapper;
  private final PlatformTransactionManager transactionManager;
  private final PurchaseProcessedMessageStore processedMessageRepository;
  private final PurchaseConfirmStatusProjectionSerivce eventAppendService;
  private final EventInfoProvider eventServiceClient;
  private final PaymentProviderRouter paymentProviderRouter;
  private final PurchaseRepository purchaseRepository;
  private final PurchasePaymentFinalizationService finalizationService;

  public PurchaseConfirmWorker(ObjectMapper objectMapper,
      @Qualifier("primaryTransactionManager") PlatformTransactionManager transactionManager,
      PurchaseProcessedMessageStore processedMessageRepository,
      PurchaseConfirmStatusProjectionSerivce eventAppendService,
      EventInfoProvider eventServiceClient, PaymentProviderRouter paymentProviderRouter,
      PurchasePaymentFinalizationService finalizationService, PurchaseRepository purchaseRepository) {
    this.objectMapper = objectMapper;
    this.transactionManager = transactionManager;
    this.processedMessageRepository = processedMessageRepository;
    this.eventAppendService = eventAppendService;
    this.eventServiceClient = eventServiceClient;
    this.paymentProviderRouter = paymentProviderRouter;
    this.finalizationService = finalizationService;
    this.purchaseRepository = purchaseRepository;
  }

  public void process(String messageId, String payloadJson) {

    if (isBlank(messageId)) {
      return;
    }

    if (!tryMarkProcessed(messageId)) {
      return;
    }

    ConfirmContext ctx = null;
    try {
      Purchase purchase = purchaseRepository.findById(extractPurchaseId(payloadJson))
          .orElseThrow(() -> new RuntimeException("결제를 찾을 수 없습니다."));

      PurchaseId purchaseId = purchase.getPurchaseId();
      if (isBlank(purchaseId.getValue())) {
        return;
      }

      ctx = loadConfirmContext(purchase, payloadJson);
      if (ctx == null) {
        recordMissingConfirmRequest(purchaseId);
        return;
      }
      if (shouldStopBeforePgCall(ctx)) {
        return;
      }

      markProcessingStarted(ctx);
      Long actualSalesVersion = loadActualSalesVersion(ctx);
      if (!ctx.expectedSalesVersion.equals(actualSalesVersion)) {
        recordEventChanged(ctx, actualSalesVersion);
        return;
      }
      markPgConfirmRequested(ctx);
      PaymentConfirmationInfo paymentInfo = confirmWithPg(ctx);
      finalizeConfirmedPayment(ctx, purchase, payloadJson, paymentInfo);
    } catch (ConcurrencyFailureException ex) {
      releaseProcessingMarker(messageId);
      throw new RuntimeException(ex.getMessage(), ex.getCause());
    } catch (Exception ex) {
      try {
        if (ctx != null) {
          recordPgConfirmFailure(ctx, ex);
        }
      } finally {
        releaseProcessingMarker(messageId);
      }
      throw ex;
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private boolean tryMarkProcessed(String messageId) {
    try {
      processedMessageRepository.save(new PurchaseProcessedMessage(messageId, LocalDateTime.now()));
      return true;
    } catch (DataIntegrityViolationException e) {
      return false;
    }
  }

  private void releaseProcessingMarker(String messageId) {
    try {
      processedMessageRepository.deleteById(messageId);
    } catch (Exception ignore) {
    }
  }

  private PurchaseId extractPurchaseId(String body) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = objectMapper.readValue(body, Map.class);
      Object val = map.get("purchaseId");
      if (val instanceof Map<?, ?> nested) {
        Object nestedValue = nested.get("value");
        return nestedValue == null ? null : new PurchaseId(nestedValue.toString());
      }
      return val == null ? null : new PurchaseId(val.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private void recordMissingConfirmRequest(PurchaseId purchaseId) {
    executeInTransaction(transactionManager, () -> {
      eventAppendService.upadteProjectionStatus(purchaseId,
          Map.of("reason", "missing_confirm_requested"),
          PurchaseConfirmStatus.FAILED, "missing confirm request");
      return null;
    });
  }

  private boolean shouldStopBeforePgCall(ConfirmContext ctx) {
    return ctx.terminal;
  }

  private void markProcessingStarted(ConfirmContext ctx) {
    executeInTransaction(transactionManager, () -> {
      eventAppendService.upadteProjectionStatus(
          new PurchaseId(ctx.purchaseId),
          Map.of("purchaseId", ctx.purchaseId),
          PurchaseConfirmStatus.PROCESSING, "processing");
      return null;
    });
  }

  private Long loadActualSalesVersion(ConfirmContext ctx) {
    EventSummary eventSummary = eventServiceClient.getEventSummary(ctx.eventId);
    return eventSummary.getSalesVersion();
  }

  private void recordEventChanged(ConfirmContext ctx, Long actualSalesVersion) {
    PurchaseId purchaseId = new PurchaseId(ctx.purchaseId);
    executeInTransaction(transactionManager, () -> {
      eventAppendService.upadteProjectionStatus(purchaseId,

          Map.of("purchaseId", purchaseId, "eventId", ctx.eventId,
              "expectedSalesVersion", ctx.expectedSalesVersion,
              "actualSalesVersion", actualSalesVersion),
          PurchaseConfirmStatus.REJECTED, "event changed; payment returned to pending");
      return null;
    });
  }

  private void markPgConfirmRequested(ConfirmContext ctx) {

    PurchaseId purchaseId = new PurchaseId(ctx.purchaseId);
    executeInTransaction(transactionManager, () -> {
      eventAppendService.upadteProjectionStatus(purchaseId,
          Map.of("provider", ctx.provider, "paymentKey", ctx.paymentKey),
          PurchaseConfirmStatus.PROCESSING, "pg confirm requested");
      return null;
    });
  }

  private PaymentConfirmationInfo confirmWithPg(ConfirmContext ctx) {
    PGApiService pgApiService = paymentProviderRouter.get(PaymentProvider.from(ctx.provider));
    return pgApiService.confirmPayment(ctx.paymentKey, ctx.orderId, ctx.amount, ctx.confirmCommandId);
  }

  private void finalizeConfirmedPayment(ConfirmContext ctx, Purchase purchase, String payloadJson,
      PaymentConfirmationInfo paymentInfo) {
    PurchaseId purchaseId = new PurchaseId(ctx.purchaseId);
    executeInTransaction(transactionManager, () -> {
      if (loadConfirmContext(purchase, payloadJson).terminal) {
        return null;
      }
      eventAppendService.upadteProjectionStatus(purchaseId,
          Map.of("paymentKey", paymentInfo.getPaymentKey(), "status", paymentInfo.getStatus()),
          PurchaseConfirmStatus.PROCESSING, "pg confirm succeeded");
      finalizationService.finalizePayment(purchaseId, paymentInfo, ctx.userId);
      eventAppendService.upadteProjectionStatus(purchaseId,
          Map.of("purchaseId", ctx.purchaseId),
          PurchaseConfirmStatus.DONE, "done");
      return null;
    });
  }

  private void recordPgConfirmFailure(ConfirmContext ctx, Exception ex) {
    PurchaseId purchaseId = new PurchaseId(ctx.purchaseId);
    executeInTransaction(transactionManager, () -> {
      purchaseRepository.findById(purchaseId)
          .filter(Purchase::isPaymentPending)
          .ifPresent(purchase -> {
            purchase.markAsFailed();
            purchaseRepository.save(purchase);
          });
      eventAppendService.upadteProjectionStatus(purchaseId,
          Map.of("error", ex.getClass().getSimpleName()),
          PurchaseConfirmStatus.FAILED, "pg confirm failed");
      return null;
    });
  }

  private ConfirmContext loadConfirmContext(Purchase purchase, String payloadJson) {

    boolean terminal = false;
    if (isTerminalEvent(purchase)) {
      terminal = true;
    }

    Map<String, Object> confirmPayload = parseJson(payloadJson);
    if (confirmPayload == null) {
      return null;
    }

    PurchaseId purchaseId = purchase.getPurchaseId();

    ConfirmContext ctx = new ConfirmContext();
    ctx.purchaseId = purchaseId.getValue();
    ctx.confirmCommandId =

        confirmCommandId(purchaseId);
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

  private boolean isTerminalEvent(Purchase purchase) {
    return !purchase.isPaymentPending();
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

  private String confirmCommandId(PurchaseId purchaseId) {
    return PurchaseConfirmCommandService.confirmCommandId(purchaseId);
  }

  private String stageCommandId(PurchaseId purchaseId, PaymentOutboxEventType eventType) {
    return confirmCommandId(purchaseId) + ":" + eventType.name();
  }

  private static class ConfirmContext {
    String purchaseId;
    String confirmCommandId;
    String userId;
    String eventId;
    Long expectedSalesVersion;
    PaymentOutboxEventType eventType;
    String paymentKey;
    String orderId;
    Integer amount;
    String provider;
    boolean terminal;
  }
}
