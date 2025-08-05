package org.codenbug.purchase.app;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.common.redis.RedisLockService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.purchase.domain.MessagePublisher;
import org.codenbug.purchase.domain.PaymentMethod;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.PaymentValidationService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseCancel;
import org.codenbug.purchase.domain.PurchaseDomainService;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundDomainService;
import org.codenbug.purchase.domain.RefundRepository;
import org.codenbug.purchase.domain.TicketRepository;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.event.RefundCompletedEvent;
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
import org.codenbug.purchase.infra.NotificationEventPublisher;
import org.codenbug.purchase.infra.PurchaseCancelRepository;
import org.codenbug.purchase.infra.PurchaseRepository;
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
	private final MessagePublisher publisher;
	private final PurchaseDomainService purchaseDomainService;
	private final PaymentValidationService paymentValidationService;
	private final NotificationEventPublisher notificationEventPublisher;
	private final RefundDomainService refundDomainService;
	private final RefundRepository refundRepository;

	/**
	 * 결제 사전 등록 처리
	 * - 결제 UUID{@code orderId}를 생성하고 결제 상태를 '진행 중'으로 설정하여 저장
	 *
	 * @param request 이벤트 ID 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userId) {
		paymentValidationService.validatePaymentRequest(request.getEventId(), request.getAmount());

		Purchase purchase = new Purchase(request.getEventId(), request.getOrderId(), request.getAmount(),
			new UserId(userId));

		purchaseRepository.save(purchase);
		return new InitiatePaymentResponse(purchase.getPurchaseId().getValue(), purchase.getPaymentStatus().name());
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
	@org.codenbug.notification.aop.NotifyUser(
		type = NotificationType.PAYMENT,
		title = "결제 완료",
		content = "티켓 결제가 성공적으로 완료되었습니다.",
		userIdExpression = "#userId"
	)
	public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, String userId) {
		try {
			Purchase purchase = purchaseRepository.findById(new PurchaseId(request.getPurchaseId()))
				.orElseThrow(() -> new IllegalArgumentException("[confirm] 구매 정보를 찾을 수 없습니다."));

			purchase.validate(request.getOrderId(), request.getAmount(), userId);

			ConfirmedPaymentInfo info = pgApiService.confirmPayment(
				request.getPaymentKey(), request.getOrderId(), request.getAmount()
			);

			PurchaseDomainService.PurchaseConfirmationResult result = 
				purchaseDomainService.confirmPurchase(purchase, info, userId);

			purchase.markAsCompleted();

			ticketRepository.saveAll(result.getTickets());
			purchaseRepository.save(purchase);
			publisher.publishSeatPurchasedEvent(
				purchase.getEventId(), 
				result.getSeatLayout().getLayoutId(), 
				result.getSeatIds(), 
				userId
			);

			PaymentMethod methodEnum = PaymentMethod.from(info.getMethod());
			LocalDateTime localDateTime = OffsetDateTime.parse(info.getApprovedAt())
				.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
				.toLocalDateTime();

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
		Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
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
	 * 유저 측 티켓 결제 취소 (DDD 리팩토링)
	 * - 도메인 서비스를 사용한 환불 처리
	 * - 결제 취소 결과 정보를 반환
	 *
	 * @param paymentKey 결제 uuid 키
	 * @param request 결제 취소 사유가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 취소 상태 정보를 포함한 응답 DTO
	 */
	@Transactional
	@org.codenbug.notification.aop.NotifyUser(
		type = NotificationType.PAYMENT,
		title = "환불 완료",
		content = "티켓 환불이 성공적으로 처리되었습니다.",
		userIdExpression = "#userId"
	)
	public CancelPaymentResponse cancelPayment(CancelPaymentRequest request, String paymentKey, String userId) {
		try {
			// 1. 구매 정보 조회
			Purchase purchase = purchaseRepository.findByPid(paymentKey)
				.orElseThrow(() -> new IllegalArgumentException("[cancel] 해당 결제 정보를 찾을 수 없습니다."));

			// 2. 외부 결제 시스템 취소 요청
			CanceledPaymentInfo canceledPaymentInfo = pgApiService.cancelPayment(paymentKey, request.getCancelReason());
			
			// 3. 환불 금액 계산
			int refundAmount = canceledPaymentInfo.getCancels().stream()
				.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
				.sum();

			// 4. 도메인 서비스를 통한 환불 처리
			RefundDomainService.RefundResult refundResult = refundDomainService.processUserRefund(
				purchase, 
				refundAmount, 
				request.getCancelReason(), 
				new UserId(userId)
			);

			// 5. 환불 엔티티 저장
			Refund savedRefund = refundRepository.save(refundResult.getRefund());
			
			// 6. 외부 결제 시스템 정보로 환불 완료 처리
			refundDomainService.completeRefundWithPaymentInfo(savedRefund, canceledPaymentInfo);
			refundRepository.save(savedRefund);

			// 7. 구매 정보 저장 (상태 변경)
			purchaseRepository.save(purchase);

			// 8. 레거시 PurchaseCancel 엔티티도 함께 저장 (호환성 유지)
			saveLegacyPurchaseCancel(purchase, canceledPaymentInfo);

			// 9. 환불 완료 알림 이벤트 발행
			publishRefundCompletedEvent(purchase, refundAmount, request.getCancelReason(), canceledPaymentInfo);

			// 10. 좌석 취소 이벤트 발행
			publisher.publishSeatPurchaseCanceledEvent(refundResult.getSeatIds(), purchase.getPurchaseId().getValue());

			return CancelPaymentResponse.of(canceledPaymentInfo);
			
		} catch (Exception e) {
			log.error("[cancelPayment] 결제 취소 처리 중 예외 발생 - userId: {}, paymentKey: {}, 오류: {}", 
				userId, paymentKey, e.getMessage(), e);
			// Redis 락 해제
			redisLockService.releaseAllLocks(userId);
			redisLockService.releaseAllEntryQueueLocks(userId);
			throw e;
		}
	}

	/**
	 * 레거시 호환성을 위한 PurchaseCancel 저장
	 */
	private void saveLegacyPurchaseCancel(Purchase purchase, CanceledPaymentInfo canceledPaymentInfo) {
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
	}

	/**
	 * 환불 완료 알림 이벤트 발행
	 */
	private void publishRefundCompletedEvent(Purchase purchase, int refundAmount, String cancelReason, 
											CanceledPaymentInfo canceledPaymentInfo) {
		try {
			RefundCompletedEvent refundEvent = RefundCompletedEvent.of(
				purchase.getUserId().getValue(),
				purchase.getPurchaseId().getValue(),
				purchase.getOrderId(),
				purchase.getOrderName(),
				refundAmount,
				cancelReason,
				canceledPaymentInfo.getCancels().get(0).getCanceledAt(),
				purchase.getOrderName()
			);

			notificationEventPublisher.publishRefundCompletedEvent(refundEvent);
		} catch (Exception e) {
			log.error("환불 완료 알림 이벤트 발행 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
				purchase.getUserId().getValue(), purchase.getPurchaseId().getValue(), e.getMessage(), e);
		}
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