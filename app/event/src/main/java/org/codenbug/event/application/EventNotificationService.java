package org.codenbug.event.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.codenbug.notification.application.service.NotificationApplicationService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event 서비스에서 알림을 전송하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final NotificationApplicationService notificationApplicationService;

    /**
     * 새 이벤트 오픈 알림 전송 (관심있는 사용자들에게)
     */
    public void sendEventOpenNotification(String eventId, String eventTitle,
                                        LocalDateTime eventDateTime, List<String> interestedUserIds) {
        String title = "[티켓온] 새 공연 오픈";
        String content = String.format(
            "새로운 공연 '%s'이(가) 오픈되었습니다!\n공연 일시: %s\n지금 바로 예매하세요!",
            eventTitle,
            eventDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        String targetUrl = String.format("/events/%d", eventId);

        for (String userId : interestedUserIds) {
            try {
                notificationApplicationService.createNotification(
                    userId,
                    NotificationType.EVENT,
                    title,
                    content,
                    targetUrl
                );
                log.info("이벤트 오픈 알림 전송 성공: userId={}, eventId={}", userId, eventId);
            } catch (Exception e) {
                log.error("이벤트 오픈 알림 전송 실패: userId={}, eventId={}", userId, eventId, e);
            }
        }
    }

    /**
     * 이벤트 시작 1시간 전 알림
     */
    public void sendEventReminderNotification(String eventId, String eventTitle,
                                            LocalDateTime eventDateTime, List<String> attendeeUserIds) {
        String title = "[티켓온] 공연 시작 알림";
        String content = String.format(
            "'%s' 공연이 1시간 후 시작됩니다.\n공연 시간: %s\n공연장에 미리 도착하시기 바랍니다.",
            eventTitle,
            eventDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        String targetUrl = String.format("/my-tickets?eventId=%d", eventId);

        for (String userId : attendeeUserIds) {
            try {
                notificationApplicationService.createNotification(
                    userId,
                    NotificationType.EVENT,
                    title,
                    content,
                    targetUrl
                );
                log.info("공연 시작 알림 전송 성공: userId={}, eventId={}", userId, eventId);
            } catch (Exception e) {
                log.error("공연 시작 알림 전송 실패: userId={}, eventId={}", userId, eventId, e);
            }
        }
    }

    /**
     * 이벤트 취소 알림
     */
    public void sendEventCancelNotification(Long eventId, String eventTitle, 
                                          String cancelReason, List<String> attendeeUserIds) {
        String title = "[티켓온] 공연 취소 안내";
        String content = String.format(
            "'%s' 공연이 취소되었습니다.\n취소 사유: %s\n환불은 자동으로 처리됩니다.",
            eventTitle,
            cancelReason
        );
        String targetUrl = "/my-tickets";

        for (String userId : attendeeUserIds) {
            try {
                notificationApplicationService.createNotification(
                    userId,
                    NotificationType.EVENT,
                    title,
                    content,
                    targetUrl
                );
                log.info("공연 취소 알림 전송 성공: userId={}, eventId={}", userId, eventId);
            } catch (Exception e) {
                log.error("공연 취소 알림 전송 실패: userId={}, eventId={}", userId, eventId, e);
            }
        }
    }

    /**
     * 좌석 예매 성공 알림
     */
    public void sendSeatReservationSuccessNotification(String userId, String eventTitle,
                                                     String seatInfo, LocalDateTime eventDateTime) {
        String title = "[티켓온] 좌석 예매 성공";
        String content = String.format(
            "'%s' 좌석 예매가 성공했습니다!\n좌석 정보: %s\n공연 일시: %s\n결제를 완료해주세요.",
            eventTitle,
            seatInfo,
            eventDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        String targetUrl = "/payment";

        try {
            notificationApplicationService.createNotification(
                userId,
                NotificationType.TICKET,
                title,
                content,
                targetUrl
            );
            log.info("좌석 예매 성공 알림 전송: userId={}, eventTitle={}", userId, eventTitle);
        } catch (Exception e) {
            log.error("좌석 예매 성공 알림 전송 실패: userId={}, eventTitle={}", userId, eventTitle, e);
        }
    }
}