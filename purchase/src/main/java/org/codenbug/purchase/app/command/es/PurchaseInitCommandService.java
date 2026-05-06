package org.codenbug.purchase.app.command.es;

import org.codenbug.purchase.app.exception.OrderIdExistException;
import java.time.LocalDateTime;
import java.util.Map;

import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.PaymentValidationService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.ui.request.InitiatePaymentRequest;
import org.codenbug.purchase.ui.response.InitiatePaymentResponse;
import org.codenbug.purchase.app.exception.OrderExistException;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PurchaseInitCommandService {
  private final ObjectMapper objectMapper;
  private final PurchaseRepository purchaseRepository;
  private final PaymentValidationService paymentValidationService;

  public PurchaseInitCommandService(
      ObjectMapper objectMapper,
      PurchaseRepository purchaseRepository,
      PaymentValidationService paymentValidationService) {
    this.objectMapper = objectMapper;
    this.purchaseRepository = purchaseRepository;
    this.paymentValidationService = paymentValidationService;
  }

  @Transactional
  public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userId) {
    paymentValidationService.validatePaymentRequest(request.getEventId(), request.getAmount());

    ensureUniqueOrderId(request.getOrderId());

    EventSummary eventSummary = paymentValidationService.getEventSummary(request.getEventId());
    UserId memberId = new UserId(userId);
    if (purchaseRepository.existsByUserIdAndPaymentStatus(memberId, PaymentStatus.IN_PROGRESS)) {
      throw new OrderExistException("결제 대기 중인 예매가 이미 존재합니다.");
    }

    Purchase purchase = new Purchase(
        request.getEventId(),
        request.getOrderId(),
        request.getAmount(),
        eventSummary.getSalesVersion(),
        memberId);

    Purchase saved = purchaseRepository.save(purchase);

    // String purchaseId = purchase.getPurchaseId().getValue();
    // String commandId = "init:" + purchaseId;
    //
    // LocalDateTime now = LocalDateTime.now();
    // String payloadJson = toJson(Map.of(
    // "purchaseId", purchaseId,
    // "userId", userId,
    // "eventId", purchase.getEventId(),
    // "orderId", purchase.getOrderId(),
    // "amount", purchase.getAmount(),
    // "expectedSalesVersion", purchase.getExpectedSalesVersion(),
    // "status", purchase.getPaymentStatus().name(),
    // "paymentDeadlineAt", purchase.getPaymentDeadlineAt().toString()));
    // String metadataJson = toJson(Map.of("commandId", commandId));
    // eventStoreRepository.save(new PurchaseStoredEvent(
    // purchaseId,
    // PurchaseEventType.PAYMENT_INITIATED.name(),
    // commandId,
    // payloadJson,
    // metadataJson,
    // now));

    return new InitiatePaymentResponse(saved.getPurchaseId().getValue(), purchase.getPaymentStatus().name(),
        purchase.getPaymentDeadlineAt(), saved.getOrderId());
  }

  private void ensureUniqueOrderId(String orderId) {
    if (purchaseRepository.existsByOrderId(orderId)) {
      throw new OrderIdExistException("이미 존재하는 orderId입니다.");
    }
  }

  // private String toJson(Object obj) {
  // try {
  // return objectMapper.writeValueAsString(obj);
  // } catch (Exception e) {
  // throw new IllegalStateException("json serialization failed", e);
  // }
  // }
}
