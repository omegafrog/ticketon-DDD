package org.codenbug.notification.service;

import org.codenbug.notification.application.service.NotificationApplicationService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * App 모듈에서 알림 기능을 사용하기 위한 클라이언트 서비스 Notification 모듈이 Bean으로 주입되어 직접 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationClientService {

    private final NotificationApplicationService notificationApplicationService;

    /**
     * 티켓 구매 완료 알림 전송
     */
    public void sendTicketPurchaseNotification(String userId, String eventTitle,
            String ticketInfo) {
        try {
            String title = "티켓 구매 완료";
            String content = String.format("%s 공연의 티켓 구매가 완료되었습니다. %s", eventTitle, ticketInfo);
            String targetUrl = "/my-tickets";

            notificationApplicationService.createNotification(userId, NotificationType.TICKET,
                    title, content, targetUrl);
            log.info("티켓 구매 완료 알림 전송 완료: userId={}, eventTitle={}", userId, eventTitle);
        } catch (Exception e) {
            log.error("티켓 구매 완료 알림 전송 실패: userId={}, eventTitle={}", userId, eventTitle, e);
        }
    }

    /**
     * 결제 완료 알림 전송
     */
    public void sendPaymentCompletedNotification(String userId, String paymentInfo, String amount) {
        try {
            String title = "결제 완료";
            String content =
                    String.format("결제가 완료되었습니다. 결제 금액: %s, 결제 정보: %s", amount, paymentInfo);
            String targetUrl = "/payment-history";

            notificationApplicationService.createNotification(userId, NotificationType.PAYMENT,
                    title, content, targetUrl);
            log.info("결제 완료 알림 전송 완료: userId={}, amount={}", userId, amount);
        } catch (Exception e) {
            log.error("결제 완료 알림 전송 실패: userId={}, amount={}", userId, amount, e);
        }
    }

    /**
     * 이벤트 오픈 알림 전송
     */
    public void sendEventOpenNotification(String userId, String eventTitle, String eventDate) {
        try {
            String title = "새로운 공연 오픈";
            String content =
                    String.format("새로운 공연 '%s'이 오픈되었습니다. 공연 일시: %s", eventTitle, eventDate);
            String targetUrl = "/events";

            notificationApplicationService.createNotification(userId, NotificationType.EVENT, title,
                    content, targetUrl);
            log.info("이벤트 오픈 알림 전송 완료: userId={}, eventTitle={}", userId, eventTitle);
        } catch (Exception e) {
            log.error("이벤트 오픈 알림 전송 실패: userId={}, eventTitle={}", userId, eventTitle, e);
        }
    }

    /**
     * 시스템 공지 알림 전송
     */
    public void sendSystemNotification(String userId, String title, String content) {
        try {
            notificationApplicationService.createNotification(userId, NotificationType.SYSTEM,
                    title, content);
            log.info("시스템 알림 전송 완료: userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.error("시스템 알림 전송 실패: userId={}, title={}", userId, title, e);
        }
    }

    /**
     * 레거시 알림 생성 (하위 호환성)
     */
    public void sendLegacyNotification(String userId, NotificationType type, String content) {
        try {
            notificationApplicationService.createLegacyNotification(userId, type, content);
            log.info("레거시 알림 전송 완료: userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.error("레거시 알림 전송 실패: userId={}, type={}", userId, type, e);
        }
    }
}
