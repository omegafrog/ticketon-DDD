package org.codenbug.purchase.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.codenbug.redislock.RedisLockService;
import org.codenbug.purchase.domain.EventProjectionRepository;
import org.codenbug.purchase.domain.MessagePublisher;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseDomainService;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Ticket;
import org.codenbug.purchase.domain.TicketRepository;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.global.ConfirmPaymentResponse;
import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.query.model.EventProjection;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceSagaTest {

    @Mock private PGApiService pgApiService;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private RedisLockService redisLockService;
    @Mock private MessagePublisher messagePublisher;
    @Mock private PurchaseDomainService purchaseDomainService;
    @Mock private EventProjectionRepository eventProjectionRepository;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private PurchaseService purchaseService;

    private Purchase testPurchase;
    private EventProjection testEvent;
    private ConfirmedPaymentInfo mockPaymentInfo;
    private String testUserId = "test-user-id";
    private String testPurchaseId = "test-purchase-id";
    private String testEventId = "test-event-id";

    @BeforeEach
    void setUp() {
        testPurchase = new Purchase(testEventId, "test-order-id", 50000, new UserId(testUserId));
        testEvent = new EventProjection(
            testEventId, "테스트 이벤트", "manager-1", 1L, 
            true, "서울", "2025-12-01T19:00:00", "2025-12-01T21:00:00", 1L, "OPEN"
        );
        
        // TransactionManager Mock 설정
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        
        ConfirmedPaymentInfo.Receipt receipt = new ConfirmedPaymentInfo.Receipt("https://receipt.url");
        mockPaymentInfo = new ConfirmedPaymentInfo(
            "test-payment-key",
            "test-order-id", 
            "테스트 이벤트 티켓",
            50000,
            "DONE",
            "카드",
            OffsetDateTime.now().toString(),
            receipt
        );
    }

    @Test
    @DisplayName("외부 API 성공 시 정상적으로 결제가 완료되어야 한다")
    void testExternalApiSuccess() {
        // Given
        PurchaseId purchaseId = new PurchaseId(testPurchaseId);
        testPurchase = spy(testPurchase);
        
        // Event 상태가 변경되지 않은 상황
        EventProjection unchangedEvent = new EventProjection(
            testEventId, "테스트 이벤트", "manager-1", 1L,
            true, "서울", "2025-12-01T19:00:00", "2025-12-01T21:00:00", 1L, "OPEN"  // 동일한 version
        );

        when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(testPurchase));
        when(eventProjectionRepository.findByEventId(testEventId)).thenReturn(testEvent, unchangedEvent);
        when(pgApiService.confirmPayment("test-payment-key", "test-order-id", 50000))
            .thenReturn(mockPaymentInfo);
        
        // PurchaseDomainService 모킹
        PurchaseDomainService.PurchaseConfirmationResult mockResult = mock(PurchaseDomainService.PurchaseConfirmationResult.class);
        when(mockResult.getTickets()).thenReturn(Arrays.asList(mock(Ticket.class)));
        when(mockResult.getSeatLayout()).thenReturn(mock(SeatLayoutProjection.class));
        when(mockResult.getSeatIds()).thenReturn(Arrays.asList("seat-1", "seat-2"));
        when(purchaseDomainService.confirmPurchase(eq(testPurchase), eq(mockPaymentInfo), eq(testUserId)))
            .thenReturn(mockResult);

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
            testPurchaseId,
            "test-payment-key",
            "test-order-id",
            50000
        );

        // When
        ConfirmPaymentResponse response = purchaseService.confirmPaymentWithSaga(request, testUserId);

        // Then
        assertNotNull(response);
        assertEquals("test-payment-key", response.getPaymentKey());
        assertEquals("test-order-id", response.getOrderId());
        assertEquals(50000, response.getTotalAmount());
        assertEquals("DONE", response.getStatus());

        // 외부 API 호출 검증
        verify(pgApiService).confirmPayment("test-payment-key", "test-order-id", 50000);
        
        // 결제 완료 처리 검증
        verify(testPurchase).markAsCompleted();
        verify(purchaseRepository, times(1)).save(testPurchase); // finalizePayment only (preparePayment는 다른 Purchase 객체)
        verify(ticketRepository).saveAll(anyList());
        verify(messagePublisher).publishSeatPurchasedEvent(eq(testEventId), any(), any(), eq(testUserId));
        
        // 보상 트랜잭션이 호출되지 않았는지 검증
        verify(pgApiService, never()).cancelPayment(anyString(), anyString());
        verify(testPurchase, never()).cancel();
        verify(testPurchase, never()).markAsFailed();
    }

    @Test
    @DisplayName("외부 API 실패 시 보상 트랜잭션이 실행되어야 한다")
    void testExternalApiFailure() {
        // Given
        PurchaseId purchaseId = new PurchaseId(testPurchaseId);
        
        when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(testPurchase));
        when(eventProjectionRepository.findByEventId(testEventId)).thenReturn(testEvent);
        when(pgApiService.confirmPayment("test-payment-key", "test-order-id", 50000))
            .thenThrow(new RuntimeException("외부 API 호출 실패"));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
            testPurchaseId,
            "test-payment-key",
            "test-order-id",
            50000
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> purchaseService.confirmPaymentWithSaga(request, testUserId));
        
        assertEquals("외부 API 호출 실패", exception.getMessage());
        
        // 보상 트랜잭션 검증  
        verify(purchaseRepository, times(1)).save(any(Purchase.class)); // compensateFailedPayment only
        verify(redisLockService, times(1)).releaseAllLocks(testUserId); // compensateFailedPayment only
        verify(redisLockService, times(1)).releaseAllEntryQueueLocks(testUserId);
        
        // 정상 완료 로직이 호출되지 않았는지 검증
        verify(ticketRepository, never()).saveAll(anyList());
        verify(messagePublisher, never()).publishSeatPurchasedEvent(anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("Event version이 변경된 경우 환불 처리가 실행되어야 한다")
    void testEventVersionChanged() {
        // Given
        PurchaseId purchaseId = new PurchaseId(testPurchaseId);
        testPurchase = spy(testPurchase);
        
        // Event version이 변경된 상황 (1L -> 2L)
        EventProjection changedEvent = new EventProjection(
            testEventId, "테스트 이벤트", "manager-1", 1L,
            true, "서울", "2025-12-01T19:00:00", "2025-12-01T21:00:00", 2L, "OPEN"  // version 변경
        );

        when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(testPurchase));
        when(eventProjectionRepository.findByEventId(testEventId)).thenReturn(testEvent, changedEvent);
        when(pgApiService.confirmPayment("test-payment-key", "test-order-id", 50000))
            .thenReturn(mockPaymentInfo);
        
        // 환불 처리 모킹
        CanceledPaymentInfo cancelInfo = new CanceledPaymentInfo();
        when(pgApiService.cancelPayment("test-payment-key", "이벤트 상태 변경으로 인한 자동 취소"))
            .thenReturn(cancelInfo);

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
            testPurchaseId,
            "test-payment-key",
            "test-order-id",
            50000
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseService.confirmPaymentWithSaga(request, testUserId));
        
        assertTrue(exception.getMessage().contains("결제 진행 중 이벤트 상태가 변경되어 결제가 취소되었습니다"));
        
        // Event version 변경 감지 및 환불 처리 검증
        verify(pgApiService).confirmPayment("test-payment-key", "test-order-id", 50000);
        verify(pgApiService).cancelPayment("test-payment-key", "이벤트 상태 변경으로 인한 자동 취소");
        verify(testPurchase).cancel();
        verify(purchaseRepository, times(1)).save(testPurchase); // compensate only (preparePayment는 더 이상 save 안 함)
        verify(redisLockService, times(2)).releaseAllLocks(testUserId); // finalizePaymentWithVersionCheck error handling + compensatePaymentDueToEventChange 
        verify(redisLockService, times(2)).releaseAllEntryQueueLocks(testUserId);
        
        // 정상 완료 로직이 호출되지 않았는지 검증
        verify(testPurchase, never()).markAsCompleted();
        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Event 상태가 OPEN이 아닌 경우 환불 처리가 실행되어야 한다")
    void testEventStatusNotOpen() {
        // Given
        PurchaseId purchaseId = new PurchaseId(testPurchaseId);
        testPurchase = spy(testPurchase);
        
        // Event 상태가 CLOSED로 변경된 상황
        EventProjection closedEvent = new EventProjection(
            testEventId, "테스트 이벤트", "manager-1", 1L,
            true, "서울", "2025-12-01T19:00:00", "2025-12-01T21:00:00", 1L, "CLOSED"  // 상태 변경
        );

        when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(testPurchase));
        when(eventProjectionRepository.findByEventId(testEventId)).thenReturn(testEvent, closedEvent);
        when(pgApiService.confirmPayment("test-payment-key", "test-order-id", 50000))
            .thenReturn(mockPaymentInfo);
        
        // 환불 처리 모킹
        CanceledPaymentInfo cancelInfo = new CanceledPaymentInfo();
        when(pgApiService.cancelPayment("test-payment-key", "이벤트 상태 변경으로 인한 자동 취소"))
            .thenReturn(cancelInfo);

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
            testPurchaseId,
            "test-payment-key",
            "test-order-id",
            50000
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseService.confirmPaymentWithSaga(request, testUserId));
        
        assertTrue(exception.getMessage().contains("결제 진행 중 이벤트 상태가 변경되어 결제가 취소되었습니다"));
        
        // Event 상태 변경 감지 및 환불 처리 검증
        verify(pgApiService).confirmPayment("test-payment-key", "test-order-id", 50000);
        verify(pgApiService).cancelPayment("test-payment-key", "이벤트 상태 변경으로 인한 자동 취소");
        verify(testPurchase).cancel();
        verify(purchaseRepository, times(1)).save(testPurchase); // compensate only (preparePayment는 더 이상 save 안 함)
    }
}