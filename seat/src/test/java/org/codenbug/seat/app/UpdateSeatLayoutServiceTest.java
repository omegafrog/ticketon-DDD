package org.codenbug.seat.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.codenbug.message.SeatLayoutUpdatedEvent;
import org.codenbug.redislock.RedisLockService;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatCancelRequest;
import org.codenbug.seat.global.SeatSelectRequest;
import org.codenbug.seat.global.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateSeatLayoutServiceTest {

  @Mock
  private SeatLayoutRepository seatLayoutRepository;

  @Mock
  private EventSeatLayoutPort eventServiceClient;

  @Mock
  private SeatTransactionService seatTransactionService;

  @Mock
  private RedisLockService redisLockService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private UpdateSeatLayoutService updateSeatLayoutService;

  private SeatLayout testSeatLayout;
  private RegisterSeatLayoutDto updateRequest;
  private Long testSeatLayoutId = 123L;

  @BeforeEach
  void setUp() {
    // SeatLayout mock 생성
    testSeatLayout = mock(SeatLayout.class);

    // 업데이트 요청 DTO 생성
    List<List<String>> layout = Arrays.asList(Arrays.asList("A1", "A2"), Arrays.asList("B1", "B2"));
    List<SeatDto> seatDtos = Arrays.asList(new SeatDto("A001", "A1", "VIP", 100000, true),
        new SeatDto("A002", "A2", "VIP", 100000, true));
    updateRequest = new RegisterSeatLayoutDto(layout, seatDtos, "Updated Location", "Updated Hall");
  }

  @Test
  @DisplayName("SeatLayout 업데이트 시 SeatLayoutUpdatedEvent가 발행되어야 한다")
  void 좌석_배치_업데이트_이벤트_발행() {
    // Given
    when(seatLayoutRepository.findSeatLayout(testSeatLayoutId)).thenReturn(testSeatLayout);
    when(seatLayoutRepository.save(testSeatLayout)).thenReturn(testSeatLayout);

    // When
    updateSeatLayoutService.update(testSeatLayoutId, updateRequest);

    // Then
    // SeatLayout이 업데이트되었는지 확인
    verify(testSeatLayout).update(any(List.class), anyList());
    verify(seatLayoutRepository).save(testSeatLayout);

    // SeatLayoutUpdatedEvent가 발행되었는지 확인
    ArgumentCaptor<SeatLayoutUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(SeatLayoutUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    SeatLayoutUpdatedEvent publishedEvent = eventCaptor.getValue();
    assertEquals(testSeatLayoutId, publishedEvent.getSeatLayoutId());
  }

  @Test
  @DisplayName("SeatLayout을 찾을 수 없으면 예외가 발생해야 한다")
  void 좌석_배치_업데이트_찾을_수_없음() {
    // Given
    when(seatLayoutRepository.findSeatLayout(testSeatLayoutId))
        .thenThrow(new RuntimeException("SeatLayout not found"));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> updateSeatLayoutService.update(testSeatLayoutId, updateRequest));

    assertEquals("SeatLayout not found", exception.getMessage());

    // 이벤트가 발행되지 않았는지 확인
    verify(eventPublisher, never()).publishEvent(any(SeatLayoutUpdatedEvent.class));
    verify(seatLayoutRepository, never()).save(any(SeatLayout.class));
  }

  @Test
  @DisplayName("SeatLayout 업데이트 중 예외 발생 시 이벤트가 발행되지 않아야 한다")
  void 좌석_배치_업데이트_예외_발생() {
    // Given
    when(seatLayoutRepository.findSeatLayout(testSeatLayoutId)).thenReturn(testSeatLayout);
    doThrow(new RuntimeException("Update failed")).when(testSeatLayout).update(any(List.class),
        anyList());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> updateSeatLayoutService.update(testSeatLayoutId, updateRequest));

    assertEquals("Update failed", exception.getMessage());

    // 이벤트가 발행되지 않았는지 확인 (트랜잭션 롤백 시나리오)
    verify(eventPublisher, never()).publishEvent(any(SeatLayoutUpdatedEvent.class));
    verify(seatLayoutRepository, never()).save(any(SeatLayout.class));
  }

  @Test
  @DisplayName("지정석 선택은 요청 좌석 수와 티켓 수가 일치해야 한다")
  void 좌석_선택_티켓_수_불일치_거부() {
    SeatLayout seatLayout = seatLayout();
    EventSeatLayoutSummary event = eventSummary(true);
    when(eventServiceClient.getEventSummary("event-1")).thenReturn(event);
    when(seatLayoutRepository.findSeatLayout(event.seatLayoutId())).thenReturn(seatLayout);

    String seatId = seatLayout.getSeats().iterator().next().getSeatId().getValue();

    assertThrows(IllegalArgumentException.class,
        () -> updateSeatLayoutService.selectSeat("event-1",
            new SeatSelectRequest(List.of(seatId), 2), "user-1"));
  }

  @Test
  @DisplayName("이미 unavailable인 지정석 선택은 거절한다")
  void 좌석_선택_불가능_좌석_거부() {
    SeatLayout seatLayout = seatLayout();
    EventSeatLayoutSummary event = eventSummary(true);
    when(eventServiceClient.getEventSummary("event-1")).thenReturn(event);
    when(seatLayoutRepository.findSeatLayout(event.seatLayoutId())).thenReturn(seatLayout);
    Seat seat = seatLayout.getSeats().iterator().next();
    seat.reserve();

    assertThrows(ConflictException.class,
        () -> updateSeatLayoutService.selectSeat("event-1",
            new SeatSelectRequest(List.of(seat.getSeatId().getValue()), 1), "user-1"));
  }

  @Test
  @DisplayName("미지정석 선택은 가능한 좌석 중 티켓 수만큼 hold한다")
  void 미지정석_선택_가능_좌석_홀드() {
    SeatLayout seatLayout = seatLayout();
    EventSeatLayoutSummary event = eventSummary(false);
    when(eventServiceClient.getEventSummary("event-1")).thenReturn(event);
    when(seatLayoutRepository.findSeatLayout(event.seatLayoutId())).thenReturn(seatLayout);

    var response = updateSeatLayoutService.selectSeat("event-1",
        new SeatSelectRequest(List.of(), 2), "user-1");

    assertEquals(2, response.getSeatList().size());
    verify(seatTransactionService, times(2)).reserveSeat(any(Seat.class), eq("user-1"), eq("event-1"), any());
  }

  @Test
  @DisplayName("좌석 release는 락이 없어도 멱등으로 available 상태를 복구한다")
  void 좌석_취소_멱등_릴리즈() {
    SeatLayout seatLayout = seatLayout();
    EventSeatLayoutSummary event = eventSummary(true);
    when(eventServiceClient.getEventSummary("event-1")).thenReturn(event);
    when(seatLayoutRepository.findSeatLayout(event.seatLayoutId())).thenReturn(seatLayout);
    Seat seat = seatLayout.getSeats().iterator().next();
    seat.reserve();
    when(redisLockService.getLockValue("seat:lock:event-1:" + seat.getSeatId().getValue())).thenReturn(null);
    when(redisLockService.unlock("seat:lock:event-1:" + seat.getSeatId().getValue(), null)).thenReturn(false);

    updateSeatLayoutService.cancelSeat("event-1",
        new SeatCancelRequest(List.of(seat.getSeatId().getValue())), "user-1");

    assertTrue(seat.isAvailable());
  }

  private SeatLayout seatLayout() {
    SeatLayout seatLayout = new SeatLayout(List.of(List.of("A1", "A2")),
        new Location("Seoul", "Hall"),
        List.of(new Seat("A1", 1000, "A"), new Seat("A2", 1000, "A")));
    ReflectionTestUtils.setField(seatLayout, "id", 1L);
    return seatLayout;
  }

  private EventSeatLayoutSummary eventSummary(boolean seatSelectable) {
    return new EventSeatLayoutSummary(1L, seatSelectable);
  }
}
