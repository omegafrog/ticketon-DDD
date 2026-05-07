package org.codenbug.purchase.app.command.es;

import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;
import java.time.LocalDateTime;
import java.util.Map;

import org.codenbug.purchase.domain.PaymentProvider;
import org.codenbug.purchase.app.event.PurchaseConfirmTransactionCommitted;
import org.codenbug.purchase.domain.ConfirmExpiredException;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.ui.response.ConfirmPaymentAcceptedResponse;
import org.codenbug.purchase.ui.request.ConfirmPaymentRequest;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PurchaseConfirmCommandService {
  public static final String CONFIRM_WORK_QUEUE = "purchase.payment.confirm.work";

  private final ObjectMapper objectMapper;
  private final PurchaseRepository purchaseRepository;
  private final PurchaseOutboxStore outboxRepository;
  private final PurchaseConfirmStatusProjectionStore statusProjectionRepository;
  private final ApplicationEventPublisher eventPublisher;

  public PurchaseConfirmCommandService(ObjectMapper objectMapper, PurchaseRepository purchaseRepository,
      PurchaseOutboxStore outboxRepository,
      PurchaseConfirmStatusProjectionStore statusProjectionRepository, ApplicationEventPublisher eventPublisher) {
    this.objectMapper = objectMapper;
    this.purchaseRepository = purchaseRepository;
    this.outboxRepository = outboxRepository;
    this.statusProjectionRepository = statusProjectionRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public ConfirmPaymentAcceptedResponse requestConfirm(ConfirmPaymentRequest request, String userId) {
    String purchaseId = request.getPurchaseId();

    Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
        .orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));

    purchase.validate(request.getOrderId(), request.getAmount(), userId);

    try {
      purchase.ensureConfirmableAt(LocalDateTime.now());
    } catch (ConfirmExpiredException e) {
      purchase.expire();
      purchaseRepository.save(purchase);
      throw e;
    }

    boolean alreadyRequested = outboxRepository.existsByPurchaseIdAndEventType(new PurchaseId(purchaseId),
        PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED);

    PurchaseOutboxMessage outboxMessage = null;
    if (!alreadyRequested) {
      try {
        outboxMessage = appendConfirmRequestedAndEnqueue(request, userId, new PurchaseId(purchaseId),
            PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED,
            purchase);
      } catch (DataIntegrityViolationException e) {
        // Concurrent accept races are treated as already accepted for the same purchase
        // confirm command.
      }
    }

    if (outboxMessage != null) {
      eventPublisher.publishEvent(
          new PurchaseConfirmTransactionCommitted(outboxMessage.getMessageId(), outboxMessage.getPayloadJson()));
    }

    String statusUrl = "/api/v1/payments/confirm/" + purchaseId + "/status";
    return new ConfirmPaymentAcceptedResponse(purchaseId, "PENDING", statusUrl);
  }

  private PurchaseOutboxMessage appendConfirmRequestedAndEnqueue(ConfirmPaymentRequest request, String userId, PurchaseId purchaseId,
      PaymentOutboxEventType eventType, Purchase purchase) {
    Long expectedSalesVersion = purchase.getExpectedSalesVersion();

    if (expectedSalesVersion == null) {
      throw new IllegalStateException("expectedSalesVersion is missing in init context. purchaseId=" + purchaseId);
    }

    LocalDateTime now = LocalDateTime.now();
    String payloadJson = getPayloadJson(request, userId, purchaseId, purchase,
        eventType, expectedSalesVersion);

    PurchaseConfirmStatusProjection projection = statusProjectionRepository.findById(purchaseId)
        .orElseGet(
            () -> PurchaseConfirmStatusProjection.pending(purchaseId, now));
    projection.update(PurchaseConfirmStatus.PENDING, "accepted", now);
    statusProjectionRepository.save(projection);

    String messageId = confirmCommandId(purchaseId);
    PurchaseOutboxMessage saved = outboxRepository
        .save(PurchaseOutboxMessage.of(messageId, CONFIRM_WORK_QUEUE, eventType,
            payloadJson, now));

    return saved;
  }

  private String getPayloadJson(ConfirmPaymentRequest request, String userId, PurchaseId purchaseId, Purchase purchase,
      PaymentOutboxEventType eventType, Long expectedSalesVersion) {
    String payloadJson = toJson(Map.of(
        "purchaseId", purchaseId.getValue(),
        "userId", userId,
        "eventId", purchase.getEventId(),
        "eventType", eventType,
        "expectedSalesVersion", expectedSalesVersion,
        "paymentKey", request.getPaymentKey(),
        "orderId", request.getOrderId(),
        "amount", request.getAmount(),
        "provider", PaymentProvider.from(request.getProvider()).name()));
    return payloadJson;
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new IllegalStateException("json serialization failed", e);
    }
  }

  public static String confirmCommandId(PurchaseId purchaseId) {
    return PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED.value + ":" + purchaseId.getValue();
  }
}
