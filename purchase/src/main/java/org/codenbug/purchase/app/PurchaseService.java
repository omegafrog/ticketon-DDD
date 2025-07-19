package org.codenbug.purchase.app;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.purchase.domain.EventId;
import org.codenbug.purchase.domain.EventProjectionRepository;
import org.codenbug.purchase.domain.MessagePublisher;
import org.codenbug.purchase.domain.PaymentMethod;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseCancel;
import org.codenbug.purchase.domain.SeatLayoutProjectionRepository;
import org.codenbug.purchase.domain.Ticket;
import org.codenbug.purchase.domain.TicketRepository;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.global.CancelPaymentRequest;
import org.codenbug.purchase.global.CancelPaymentResponse;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.global.ConfirmPaymentResponse;
import org.codenbug.purchase.global.InitiatePaymentRequest;
import org.codenbug.purchase.global.InitiatePaymentResponse;
import org.codenbug.purchase.global.PurchaseHistoryDetailResponse;
import org.codenbug.purchase.global.PurchaseHistoryListResponse;
import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;
import org.codenbug.purchase.infra.PurchaseCancelRepository;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.query.model.EventProjection;
import org.codenbug.purchase.query.model.Seat;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {
	private final PGApiService pgApiService;
	private final PurchaseRepository purchaseRepository;
	private final PurchaseCancelRepository purchaseCancelRepository;
	private final TicketRepository ticketRepository;
	private final RedisLockService redisLockService;
	private final EventProjectionRepository eventProjectionRepository;
	private final SeatLayoutProjectionRepository seatLayoutProjectionRepository;
	private final MessagePublisher publisher;

	/**
	 * 결제 사전 등록 처리
	 * - 결제 UUID{@code orderId}를 생성하고 결제 상태를 '진행 중'으로 설정하여 저장
	 *
	 * @param request 이벤트 ID 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userId) {

		if (!eventProjectionRepository.existById(request.getEventId()))
			throw new IllegalArgumentException("해당 이벤트가 존재하지 않습니다.");

		if (request.getAmount() <= 0)
			throw new IllegalArgumentException("결제 금액이 잘못되었습니다.");

		Purchase purchase = new Purchase(request.getEventId(), request.getOrderId(), request.getAmount(),
			new UserId(userId));

		purchaseRepository.save(purchase);
		return new InitiatePaymentResponse(purchase.getPurchaseId(), purchase.getPaymentStatus().name());
	}

	/**
	 * 결제 승인
	 * - 결제 승인 시 구매 정보를 갱신하고 티켓을 생성한 후 결제 응답을 반환합니다.
	 *
	 * @param request    결제 승인 결과 정보
	 * @param userId     현재 로그인한 사용자 ID
	 * @return 결제 UUID, 금액, 결제 수단, 승인 시각 등을 포함한 응답 DTO
	 */
	@Transactional
	public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, String userId) {
		try {
			Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
				.orElseThrow(() -> new IllegalArgumentException("[confirm] 구매 정보를 찾을 수 없습니다."));

			purchase.validate(request.getOrderId(), request.getAmount(), userId);

			String eventId = purchase.getEventId();
			List<String> seatIds = redisLockService.getLockedSeatIdsByUserId(userId);

			EventProjection eventProjection = eventProjectionRepository.findByEventId(eventId);

			// seat가 모두 있는지 확인
			// 좌석의 동시성은 좌석 선택 api를 통해 락으로 검증됨
			SeatLayoutProjection seatLayout = seatLayoutProjectionRepository.findById(
				eventProjection.getSeatLayoutId());
			List<Seat> seats = seatLayout.getSeats().stream().toList();
			for (String seatId : seatIds) {
				if (seats.stream().filter(seat -> seat.getSeatId().equals(seatId)).findFirst().isEmpty()) {
					throw new IllegalArgumentException("존재하지 않는 좌석을 선택했습니다.");
				}
			}
			if (seats.size() != seatIds.size())
				throw new IllegalArgumentException("존재하지 않는 좌석을 선택했습니다.");

			List<Ticket> tickets = seats.stream()
				.map(seat -> {
					return new Ticket(seatLayout.getLocation(), new EventId(eventProjection.getEventId()),
						seat.getSeatId(), purchase);
				})
				.toList();

			ConfirmedPaymentInfo info = pgApiService.confirmPayment(
				request.getPaymentUuid(), request.getOrderId(), request.getAmount()
			);

			PaymentMethod methodEnum = PaymentMethod.from(info.getMethod());

			LocalDateTime localDateTime = OffsetDateTime.parse(info.getApprovedAt())
				.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
				.toLocalDateTime();

			purchase.updatePaymentInfo(
				info.getPaymentKey(),
				info.getOrderId(),
				info.getTotalAmount(),
				methodEnum,
				eventProjection.isSeatSelectable() ? "지정석 %d매".formatted(seatIds.size()) :
					"미지정석 %d매".formatted(seatIds.size()),
				localDateTime
			);



			// 결제 완료 알림 생성 이벤트 생성
			// try {
			// 	String notificationTitle = String.format("[%s] 결제 완료", purchase.getOrderName());
			// 	String notificationContent = String.format("결제가 완료되었습니다.\n금액: %d원\n결제수단: %s",
			// 		purchase.getAmount(),
			// 		methodEnum.name()
			// 	);
			// 	String targetUrl = String.format("/my");
			//
			// 	notificationService.createNotification(userId, NotificationEnum.PAYMENT, notificationTitle,
			// 		notificationContent, targetUrl);
			// } catch (Exception e) {
			// 	log.error("결제 완료 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
			// 		userId, purchase.getId(), e.getMessage(), e);
			// 	// 알림 발송 실패는 결제 성공에 영향을 주지 않도록 예외를 무시함
			// }

			ticketRepository.saveAll(tickets);
			purchaseRepository.save(purchase);
			publisher.publishSeatPurchasedEvent(eventId, seatLayout.getLayoutId(), seatIds, userId);

			return new ConfirmPaymentResponse(
				info.getPaymentKey(),
				info.getOrderId(),
				info.getOrderName(),
				info.getTotalAmount(),
				info.getStatus(),
				methodEnum,
				localDateTime,
				new ConfirmPaymentResponse.Receipt(info.getReceipt().getUrl())
			);
		} catch (Exception e) {
			log.error("[confirmPayment] 결제 처리 중 예외 발생 - userId: {}, 오류: {}", userId, e.getMessage(), e);
			redisLockService.releaseAllLocks(userId);
			redisLockService.releaseAllEntryQueueLocks(userId);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 사용자의 구매 이력 목록을 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @param pageable 페이지네이션 정보
	 * @return 구매 이력 목록 응답 DTO
	 */
	@Transactional
	public PurchaseHistoryListResponse getPurchaseHistoryList(String userId, Pageable pageable) {
		Page<Purchase> purchases = purchaseRepository.findByUserIdAndPaymentStatusInOrderByCreatedAtDesc(
			new UserId(userId),
			List.of(PaymentStatus.DONE, PaymentStatus.EXPIRED),
			pageable
		);

		Page<PurchaseHistoryListResponse.PurchaseSummaryDto> purchaseDtos = purchases.map(
			PurchaseHistoryListResponse.PurchaseSummaryDto::of
		);

		return PurchaseHistoryListResponse.of(purchaseDtos);
	}

	/**
	 * 사용자의 특정 구매 이력 상세 정보를 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @param purchaseId 구매 ID
	 * @return 구매 이력 상세 응답 DTO
	 */
	@Transactional
	public PurchaseHistoryDetailResponse getPurchaseHistoryDetail(String userId, String purchaseId) {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));

		if (!purchase.getUserId().getValue().equals(userId)) {
			throw new AccessDeniedException("해당 구매 정보에 대한 접근 권한이 없습니다.");
		}

		PurchaseHistoryDetailResponse.PurchaseDto purchaseDto = PurchaseHistoryDetailResponse.PurchaseDto.of(purchase);

		log.info("purchase paymentKey: {}", purchaseDto.getPaymentKey());

		return PurchaseHistoryDetailResponse.builder()
			.purchases(List.of(purchaseDto))
			.build();
	}

	/**
	 * 유저 측 티켓 결제 취소
	 * - 전액 또는 부분 취소 요청 시 Toss 결제 취소 API 호출
	 * - 결제 취소 결과 정보를 반환
	 *
	 * @param paymentKey 결제 uuid 키
	 * @param request 결제 취소 사유가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 취소 상태 정보를 포함한 응답 DTO
	 */
	@Transactional
	public CancelPaymentResponse cancelPayment(CancelPaymentRequest request, String paymentKey, String userId) {
		Purchase purchase = purchaseRepository.findByPid(paymentKey)
			.orElseThrow(() -> new IllegalArgumentException("[cancel] 해당 결제 정보를 찾을 수 없습니다."));

		if (!purchase.getUserId().getValue().equals(userId))
			throw new AccessDeniedException("해당 구매 정보에 대한 접근 권한이 없습니다.");

		CanceledPaymentInfo canceledPaymentInfo = pgApiService.cancelPayment(paymentKey,
			request.getCancelReason());

		// TODO : 취소시 seat available을 true로 돌리는 이벤트 발행
		List<String> seatIds = purchase.getTickets().stream().map(ticket -> ticket.getSeatId()).toList();
		purchase.getTickets().clear();

		for (CanceledPaymentInfo.CancelDetail cancelDetail : canceledPaymentInfo.getCancels()) {
			PurchaseCancel purchaseCancel = PurchaseCancel.builder()
				.purchase(purchase)
				.cancelAmount(cancelDetail.getCancelAmount())
				.cancelReason(cancelDetail.getCancelReason())
				.canceledAt(OffsetDateTime.parse(cancelDetail.getCanceledAt()).toLocalDateTime())
				.receiptUrl(canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
				.build();

			purchaseCancelRepository.save(purchaseCancel);
		}

		// 환불 완료 알림 생성
		// try {
		// 	int refundAmount = canceledPaymentInfo.getCancels().stream()
		// 		.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
		// 		.sum();
		//
		// 	String notificationTitle = String.format("[%s] 환불 완료", purchase.getOrderName());
		// 	String notificationContent = String.format(
		// 		"환불 처리가 완료되었습니다.\n환불 금액: %d원",
		// 		refundAmount
		// 	);
		// 	String targetUrl = String.format("/my");
		//
		// 	notificationService.createNotification(userId, NotificationEnum.PAYMENT, notificationTitle,
		// 		notificationContent, targetUrl);
		// } catch (Exception e) {
		// 	log.error("환불 완료 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
		// 		userId, purchase.getId(), e.getMessage(), e);
		// 	// 알림 발송 실패는 환불 처리에 영향을 주지 않도록 예외를 무시함
		// }

		publisher.publishSeatPurchaseCanceledEvent(seatIds, purchase.getPurchaseId().getValue());

		return CancelPaymentResponse.of(canceledPaymentInfo);
	}

	// @Transactional
	// public List<ManagerRefundResponse> managerCancelPayment(ManagerRefundRequest request, Long eventId, User manager) {
	// 	List<Event> eventsByManager = managerEventRepository.findEventsByManager(manager);
	//
	// 	boolean hasPermission = eventsByManager.stream()
	// 		.anyMatch(event -> event.getEventId().equals(eventId));
	//
	// 	if (!hasPermission) {
	// 		throw new IllegalArgumentException("요청 매니저는 해당 이벤트에 대한 권한이 없습니다.");
	// 	}
	//
	// 	// 환불할 purchaseId 목록 결정
	// 	List<Purchase> purchasesToRefund;
	// 	if (request.isTotalRefund()) {
	// 		purchasesToRefund = purchaseRepository.findAllByEventId(eventId);
	// 	} else {
	// 		purchasesToRefund = request.getPurchasesIds().stream()
	// 			.map(id -> purchaseRepository.findById(id)
	// 				.orElseThrow(() -> new IllegalArgumentException("해당 구매 이력이 존재하지 않습니다. ID: " + id)))
	// 			.filter(p -> {
	// 				Long ticketEventId = p.getTickets().getFirst().getEvent().getEventId();
	// 				if (!ticketEventId.equals(eventId)) {
	// 					throw new IllegalArgumentException("요청한 매니저의 이벤트와 결제 티켓의 이벤트가 일치하지 않습니다.");
	// 				}
	// 				return true;
	// 			})
	// 			.toList();
	// 	}
	//
	// 	List<ManagerRefundResponse> responseList = new ArrayList<>();
	//
	// 	for (Purchase purchase : purchasesToRefund) {
	// 		// Toss 결제 취소
	// 		CanceledPaymentInfo canceledPaymentInfo = tossPaymentService.cancelPayment(
	// 			purchase.getPid(),
	// 			request.getReason()
	// 		);
	//
	// 		// 좌석 초기화 및 티켓 삭제
	// 		List<Ticket> tickets = ticketRepository.findAllByPurchaseId(purchase.getId());
	// 		List<Long> ticketIds = new ArrayList<>();
	//
	// 		for (Ticket ticket : tickets) {
	// 			ticketIds.add(ticket.getId());
	//
	// 			List<Seat> seats = seatRepository.findByTicketId(ticket.getId());
	// 			for (Seat seat : seats) {
	// 				seat.setTicket(null);
	// 				seat.setAvailable(true);
	// 				seatRepository.save(seat);
	// 			}
	// 			ticketRepository.delete(ticket);
	// 		}
	//
	// 		// PurchaseCancel 저장
	// 		for (CanceledPaymentInfo.CancelDetail cancelDetail : canceledPaymentInfo.getCancels()) {
	// 			PurchaseCancel purchaseCancel = PurchaseCancel.builder()
	// 				.purchase(purchase)
	// 				.cancelAmount(cancelDetail.getCancelAmount())
	// 				.cancelReason(cancelDetail.getCancelReason())
	// 				.canceledAt(OffsetDateTime.parse(cancelDetail.getCanceledAt()).toLocalDateTime())
	// 				.receiptUrl(
	// 					canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
	// 				.build();
	//
	// 			purchaseCancelRepository.save(purchaseCancel);
	// 		}
	//
	// 		// 각 사용자에게 환불 알림 전송
	// 		try {
	// 			int refundAmount = canceledPaymentInfo.getCancels().stream()
	// 				.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
	// 				.sum();
	//
	// 			String notificationTitle = String.format("[%s] 매니저 환불 처리", purchase.getOrderName());
	// 			String notificationContent = String.format(
	// 				"매니저에 의해 환불이 처리되었습니다.\n사유: %s\n환불 금액: %d원",
	// 				request.getReason(),
	// 				refundAmount
	// 			);
	//
	// 			// 각 구매자에게 개별 알림 전송
	// 			notificationService.createNotification(
	// 				purchase.getUserId().getUserId(),
	// 				NotificationEnum.PAYMENT,
	// 				notificationTitle,
	// 				notificationContent
	// 			);
	// 		} catch (Exception e) {
	// 			log.error("매니저 환불 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
	// 				purchase.getUserId().getUserId(), purchase.getId(), e.getMessage(), e);
	// 			// 알림 발송 실패는 환불 처리에 영향을 주지 않도록 예외를 무시함
	// 		}
	//
	// 		// 응답 DTO 생성
	// 		ManagerRefundResponse response = ManagerRefundResponse.builder()
	// 			.purchaseId(purchase.getId())
	// 			.userId(purchase.getUserId().getUserId())
	// 			.paymentStatus(purchase.getPaymentStatus())
	// 			.ticketId(ticketIds)
	// 			.refundAmount(canceledPaymentInfo.getCancels().stream()
	// 				.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
	// 				.sum())
	// 			.refundDate(OffsetDateTime.parse(
	// 				canceledPaymentInfo.getCancels().getLast().getCanceledAt()
	// 			).toLocalDateTime())
	// 			.build();
	//
	// 		responseList.add(response);
	// 	}
	//
	// 	return responseList;
	// }

}