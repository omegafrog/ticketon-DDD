package org.codenbug.seat.app;

import static org.codenbug.seat.app.SeatTransactionService.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codenbug.common.redis.RedisLockService;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.codenbug.seat.global.SeatCancelRequest;
import org.codenbug.seat.global.SeatSelectRequest;
import org.codenbug.seat.global.SeatSelectResponse;
import org.codenbug.seat.global.exception.ConflictException;
import org.codenbug.seat.query.model.EventProjection;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UpdateSeatLayoutService {
	private final SeatLayoutRepository seatLayoutRepository;
	private final EventProjectionRepository eventProjectionRepository;
	private final SeatTransactionService seatTransactionService;
	private final RedisLockService redisLockService;

	public UpdateSeatLayoutService(SeatLayoutRepository seatLayoutRepository,
		EventProjectionRepository eventProjectionRepository, SeatTransactionService seatTransactionService,
		RedisLockService redisLockService) {
		this.seatLayoutRepository = seatLayoutRepository;
		this.eventProjectionRepository = eventProjectionRepository;
		this.seatTransactionService = seatTransactionService;
		this.redisLockService = redisLockService;
	}

	public void update(Long seatLayoutId, RegisterSeatLayoutDto seatLayout) {
		SeatLayout layout = seatLayoutRepository.findSeatLayout(seatLayoutId);
		layout.update(seatLayout.getLayout(), seatLayout.getSeats()
			.stream()
			.map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
			.toList());
	}

	/**
	 * 좌석 선택 요청에 따라 Redis 락을 걸고, DB에 좌석 상태 반영
	 *
	 * @param eventId           이벤트 ID
	 * @param seatSelectRequest 선택한 좌석 ID 목록을 포함한 요청 객체
	 * @param userId            유저 ID
	 * @throws IllegalStateException 이미 선택된 좌석이 있는 경우
	 * @throws IllegalArgumentException 존재하지 않는 좌석이 포함된 경우
	 */
	@Transactional
	public SeatSelectResponse selectSeat(String eventId, SeatSelectRequest seatSelectRequest, String userId) {
		if (userId == null ) {
			throw new IllegalArgumentException("로그인된 사용자가 없습니다.");
		}

		EventProjection event = eventProjectionRepository.findByEventId(eventId)
			.orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다."));

		SeatLayout seatLayout = seatLayoutRepository.findSeatLayoutByEventId(eventId);
		Set<Seat> seats = seatLayout.getSeats();
		List<Seat> selectedSeats = seats.stream().filter(seat -> seatSelectRequest.getSeatList().stream().anyMatch(
			seatId -> seatId.equals(seat.getSeatId().getValue())
		)).toList();

		List<String> reservedSeatIds;

		if (event.getSeatSelectable()) {
			// 지정석 예매 처리
			if (selectedSeats != null && selectedSeats.size() > 4) {
				throw new IllegalArgumentException("최대 4개의 좌석만 선택할 수 있습니다.");
			}
			reservedSeatIds = selectSeats(selectedSeats, userId, eventId, true, seatSelectRequest.getTicketCount());
		} else {
			// 미지정석 예매 처리
			if (selectedSeats != null && !selectedSeats.isEmpty()) {
				throw new IllegalArgumentException("[selectSeats] 미지정석 예매 시 좌석 목록은 제공되지 않아야 합니다.");
			}
			reservedSeatIds = selectSeats(null, userId, eventId, false, seatSelectRequest.getTicketCount());
		}

		SeatSelectResponse seatSelectResponse = new SeatSelectResponse();
		seatSelectResponse.setSeatList(reservedSeatIds);
		return seatSelectResponse;
	}

	/**
	 * 지정석 또는 미지정석 선택 처리
	 *
	 * @param selectedSeats 좌석 목록 (지정석일 경우 사용)
	 * @param userId        유저 ID
	 * @param eventId       이벤트 ID
	 * @param isDesignated  지정석 여부
	 * @param ticketCount   예매할 좌석 수 (미지정석 예매 시 사용)
	 */
	private List<String> selectSeats(List<Seat> selectedSeats, String userId, String eventId, boolean isDesignated,
		int ticketCount) {
		List<String> reservedSeatIds = new ArrayList<>();
		log.info("selectedSeats: {}", selectedSeats);
		if (isDesignated) {
			// 지정석 예매 처리
			for (Seat seat : selectedSeats) {
				String seatId = seat.getSeatId().getValue();
				if (!seat.isAvailable()) {
					throw new ConflictException("[selectSeats] 이미 예매된 좌석입니다. seatId = " + seat.getSeatId().getValue());
				}

				reserveSeat(seat, userId, eventId, seatId);
				reservedSeatIds.add(seatId);
			}
		} else {
			// 미지정석 예매 처리
			List<Seat> availableSeats = selectedSeats
				.stream()
				.limit(ticketCount)
				.toList();

			if (availableSeats.size() < ticketCount) {
				throw new ConflictException("[selectSeats] 예매 가능한 좌석 수가 부족합니다.");
			}

			for (Seat seat : availableSeats) {
				reserveSeat(seat, userId, eventId, seat.getSeatId().getValue());
				reservedSeatIds.add(seat.getSeatId().getValue());
			}
		}
		return reservedSeatIds;
	}

	/**
	 * 좌석을 예약하고 Redis 락을 관리하는 공통 로직
	 *
	 * @param seat     좌석 객체
	 * @param userId   유저 ID
	 * @param eventId  이벤트 ID
	 * @param seatId   좌석 ID
	 */
	public void reserveSeat(Seat seat, String userId, String eventId, String seatId) {
		seatTransactionService.reserveSeat(seat, userId, eventId, seatId);
	}

	/**
	 * 좌석 취소 요청에 따라 Redis 락을 해제하고, DB에 좌석 상태 반영
	 *
	 * @param eventId           이벤트 ID
	 * @param seatCancelRequest 선택한 좌석 ID 목록을 포함한 요청 객체
	 * @param userId            유저 ID
	 * @throws IllegalArgumentException 존재하지 않는 좌석이 포함된 경우
	 */
	@Transactional
	public void cancelSeat(String eventId, SeatCancelRequest seatCancelRequest, String userId) {
		if (userId == null ) {
			throw new IllegalArgumentException("[cancelSeat] 로그인된 사용자가 없습니다.");
		}
		SeatLayout seatLayout = seatLayoutRepository.findSeatLayoutByEventId(eventId);

		for (String seatId : seatCancelRequest.getSeatList()) {
			String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;

			String lockValue = redisLockService.getLockValue(lockKey);

			boolean unlockSuccess = redisLockService.unlock(lockKey, lockValue);
			Seat selectedSeat = seatLayout.getSeats()
				.stream()
				.filter(seat -> seat.getSeatId().getValue().equals(seatId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("[cancelSeat] 존재하지 않는 좌석입니다."));
			if (!unlockSuccess) {
				throw new IllegalArgumentException("[cancelSeat] 좌석 락을 해제할 수 없습니다.");
			}
			selectedSeat.cancelReserve();
		}
	}
}
