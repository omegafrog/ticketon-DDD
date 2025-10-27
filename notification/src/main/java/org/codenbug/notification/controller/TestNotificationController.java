package org.codenbug.notification.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 통합된 Notification 테스트 컨트롤러 일반 알림과 환불 알림 테스트 기능을 모두 제공
 */
@RestController
@RequestMapping("/api/v1/test-notifications")
@RequiredArgsConstructor
public class TestNotificationController {

    // private final NotificationClientService notificationClientService;
    // private final NotificationApplicationService notificationApplicationService;
    // private final NotificationEventPublisher notificationEventPublisher;
    //
    // @PostMapping("/ticket-purchase")
    // public RsData<Void> sendTicketPurchaseNotification(
    // @RequestParam Long userId,
    // @RequestParam String eventTitle,
    // @RequestParam String ticketInfo) {
    //
    // notificationClientService.sendTicketPurchaseNotification(userId, eventTitle, ticketInfo);
    // return new RsData<>("200-SUCCESS", "티켓 구매 알림 전송 완료", null);
    // }
    //
    // @PostMapping("/payment-completed")
    // public RsData<Void> sendPaymentCompletedNotification(
    // @RequestParam Long userId,
    // @RequestParam String paymentInfo,
    // @RequestParam String amount) {
    //
    // notificationClientService.sendPaymentCompletedNotification(userId, paymentInfo, amount);
    // return new RsData<>("200-SUCCESS", "결제 완료 알림 전송 완료", null);
    // }
    //
    // @PostMapping("/event-open")
    // public RsData<Void> sendEventOpenNotification(
    // @RequestParam Long userId,
    // @RequestParam String eventTitle,
    // @RequestParam String eventDate) {
    //
    // notificationClientService.sendEventOpenNotification(userId, eventTitle, eventDate);
    // return new RsData<>("200-SUCCESS", "이벤트 오픈 알림 전송 완료", null);
    // }
    //
    // @PostMapping("/system")
    // public RsData<Void> sendSystemNotification(
    // @RequestParam Long userId,
    // @RequestParam String title,
    // @RequestParam String content) {
    //
    // notificationClientService.sendSystemNotification(userId, title, content);
    // return new RsData<>("200-SUCCESS", "시스템 알림 전송 완료", null);
    // }
    //
    // @PostMapping("/legacy")
    // public RsData<Void> sendLegacyNotification(
    // @RequestParam Long userId,
    // @RequestParam NotificationType type,
    // @RequestParam String content) {
    //
    // notificationClientService.sendLegacyNotification(userId, type, content);
    // return new RsData<>("200-SUCCESS", "레거시 알림 전송 완료", null);
    // }
    //
    // /**
    // * 직접 알림 생성 테스트 (Bean 주입 확인)
    // */
    // @PostMapping("/direct")
    // public RsData<NotificationDto> createDirectNotification(
    // @RequestParam String userId,
    // @RequestParam(defaultValue = "SYSTEM") NotificationType type,
    // @RequestParam(defaultValue = "직접 생성 테스트") String title,
    // @RequestParam(defaultValue = "Bean 주입이 정상적으로 작동하는지 테스트") String content) {
    //
    // NotificationDto notification = notificationApplicationService.createNotification(
    // userId, type, title, content);
    //
    // return new RsData<>("200-SUCCESS", "직접 알림 생성 완료", notification);
    // }
    //
    // /**
    // * 알림 개수 조회 테스트
    // */
    // @GetMapping("/count/{userId}")
    // public RsData<Long> getNotificationCount(@PathVariable String userId) {
    // long count = notificationApplicationService.getUnreadCount(userId);
    // return new RsData<>("200-SUCCESS", "알림 개수 조회 완료", count);
    // }
    //
    // /**
    // * 사용자 환불 완료 이벤트 테스트
    // */
    // @PostMapping("/user-refund")
    // public String testUserRefundEvent(
    // @RequestParam String userId,
    // @RequestParam(defaultValue = "ORDER-12345") String orderId,
    // @RequestParam(defaultValue = "테스트 공연 티켓") String orderName,
    // @RequestParam(defaultValue = "50000") Integer refundAmount,
    // @RequestParam(defaultValue = "사용자 요청") String refundReason) {
    //
    // RefundCompletedEvent event = RefundCompletedEvent.of(
    // userId,
    // "PURCHASE-" + System.currentTimeMillis(),
    // orderId,
    // orderName,
    // refundAmount,
    // refundReason,
    // OffsetDateTime.now().toString(),
    // orderName
    // );
    //
    // notificationEventPublisher.publishRefundCompletedEvent(event);
    // return "사용자 환불 완료 이벤트 발행 완료: " + userId;
    // }
    //
    // /**
    // * 매니저 환불 완료 이벤트 테스트
    // */
    // @PostMapping("/manager-refund")
    // public String testManagerRefundEvent(
    // @RequestParam String userId,
    // @RequestParam(defaultValue = "ORDER-67890") String orderId,
    // @RequestParam(defaultValue = "테스트 콘서트 티켓") String orderName,
    // @RequestParam(defaultValue = "75000") Integer refundAmount,
    // @RequestParam(defaultValue = "공연 취소") String refundReason,
    // @RequestParam(defaultValue = "관리자") String managerName) {
    //
    // ManagerRefundCompletedEvent event = ManagerRefundCompletedEvent.of(
    // userId,
    // "PURCHASE-" + System.currentTimeMillis(),
    // orderId,
    // orderName,
    // refundAmount,
    // refundReason,
    // OffsetDateTime.now().toString(),
    // orderName,
    // managerName
    // );
    //
    // notificationEventPublisher.publishManagerRefundCompletedEvent(event);
    // return "매니저 환불 완료 이벤트 발행 완료: " + userId + " by " + managerName;
    // }
}
