package org.codenbug.purchase.app.es;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.codenbug.purchase.app.command.es.PurchaseInitCommandService;
import org.codenbug.purchase.app.exception.OrderIdExistException;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.PaymentValidationService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.ui.request.InitiatePaymentRequest;
import org.codenbug.purchase.ui.response.InitiatePaymentResponse;
import org.springframework.dao.DataIntegrityViolationException;
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

  @InjectMocks
  private PurchaseInitCommandService service;

  @Test
  void initiatePayment_savesPurchase() {
    InitiatePaymentRequest request = new InitiatePaymentRequest("event-1", "order-1", 12000);
    EventSummary summary = new EventSummary("event-1", 10L, true, "OPEN", 3L, 7L, "title");

    when(purchaseRepository.existsByOrderId("order-1")).thenReturn(false);
    when(purchaseRepository.existsByUserIdAndPaymentStatus(any(), eq(PaymentStatus.IN_PROGRESS))).thenReturn(false);
    when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentValidationService.getEventSummary("event-1")).thenReturn(summary);

    InitiatePaymentResponse response = service.initiatePayment(request, "user-1");

    ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
    verify(purchaseRepository).save(purchaseCaptor.capture());
    Purchase savedPurchase = purchaseCaptor.getValue();

    assertEquals(savedPurchase.getPurchaseId().getValue(), response.getPurchaseId());
    assertEquals(savedPurchase.getPaymentStatus().name(), response.getStatus());
    assertEquals(savedPurchase.getOrderId(), response.getOrderId());
    verify(paymentValidationService).validatePaymentRequest("event-1", 12000);
  }

  @Test
  void initiatePayment_rejectsDuplicateOrderIdBeforeSaving() {
    InitiatePaymentRequest request = new InitiatePaymentRequest("event-1", "order-1", 12000);

    when(purchaseRepository.existsByOrderId("order-1")).thenReturn(true);

    assertThrows(OrderIdExistException.class, () -> service.initiatePayment(request, "user-1"));

    verify(purchaseRepository, never()).save(any());
    verify(purchaseRepository, never()).existsByUserIdAndPaymentStatus(any(), any());
    verify(paymentValidationService).validatePaymentRequest("event-1", 12000);
  }

  @Test
  void initiatePayment_translatesSaveRaceDuplicateOrderId() {
    InitiatePaymentRequest request = new InitiatePaymentRequest("event-1", "order-1", 12000);
    EventSummary summary = new EventSummary("event-1", 10L, true, "OPEN", 3L, 7L, "title");

    when(purchaseRepository.existsByOrderId("order-1")).thenReturn(false);
    when(purchaseRepository.existsByUserIdAndPaymentStatus(any(), eq(PaymentStatus.IN_PROGRESS))).thenReturn(false);
    when(paymentValidationService.getEventSummary("event-1")).thenReturn(summary);
    when(purchaseRepository.save(any(Purchase.class))).thenThrow(new DataIntegrityViolationException("order_id"));

    assertThrows(OrderIdExistException.class, () -> service.initiatePayment(request, "user-1"));
  }
}
