package org.codenbug.notification.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.service.NotificationDomainService;
import org.codenbug.notification.dto.NotificationDto;
import org.codenbug.notification.dto.NotificationEventDto;
import org.codenbug.notification.infrastructure.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDomainService domainService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(String userId, Pageable pageable) {
        UserId userIdVO = new UserId(userId);
        Page<Notification> notifications =
            notificationRepository.findByUserIdOrderBySentAtDesc(userIdVO, pageable);
        return notifications.map(NotificationDto::from);
    }

    public NotificationDto getNotificationById(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));

        domainService.validateUserOwnership(notification, userId);

        if (domainService.canMarkAsRead(notification)) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }

        return NotificationDto.from(notification);
    }

    public NotificationDto createNotification(String userId, NotificationType type, String title,
        String content, String targetUrl) {
        log.debug("알림 생성 시작: userId={}, type={}, title={}", userId, type, title);

        // 사용자 검증 생략 - 알림 모듈은 단순히 알림 생성만 담당

        Notification notification =
            domainService.createNotification(userId, type, title, content, targetUrl);
        Notification savedNotification = notificationRepository.save(notification);

        log.debug("알림 저장 완료: notificationId={}", savedNotification.getId());

        NotificationDto notificationDto = NotificationDto.from(savedNotification);
        NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
//        eventPublisher.publishEvent(eventDto);

        log.debug("알림 이벤트 발행 완료: notificationId={}", savedNotification.getId());

        return notificationDto;
    }

    public NotificationDto createNotification(String userId, NotificationType type, String title,
        String content) {
        return createNotification(userId, type, title, content, null);
    }

    public NotificationDto createLegacyNotification(String userId, NotificationType type,
        String content) {
        // 사용자 검증 생략 - 알림 모듈은 단순히 알림 생성만 담당

        Notification notification = domainService.createLegacyNotification(userId, type, content);
        Notification savedNotification = notificationRepository.save(notification);

        NotificationDto notificationDto = NotificationDto.from(savedNotification);
        NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
//        eventPublisher.publishEvent(eventDto);

        return notificationDto;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUnreadNotifications(String userId, Pageable pageable) {
        UserId userIdVO = new UserId(userId);
        return notificationRepository
            .findByUserIdAndIsReadFalseOrderBySentAtDesc(userIdVO, pageable)
            .map(NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        UserId userIdVO = new UserId(userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userIdVO);
    }

    public boolean retryFailedNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);

        if (notification == null || !domainService.canRetry(notification)) {
            return false;
        }

        notification.retry();
        notificationRepository.save(notification);

        NotificationEventDto eventDto = NotificationEventDto.from(notification);
//        eventPublisher.publishEvent(eventDto);

        return true;
    }

    public void deleteNotification(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));

        domainService.validateUserOwnership(notification, userId);

        notificationRepository.delete(notification);
        log.debug("알림 삭제 완료: notificationId={}, userId={}", notificationId, userId);
    }

    public void deleteNotifications(List<Long> notificationIds, String userId) {
        UserId userIdVO = new UserId(userId);
        List<Notification> notifications =
            notificationRepository.findAllByUserIdAndIdIn(userIdVO, notificationIds);

        if (notifications.size() < notificationIds.size()) {
            log.info("요청된 알림 중 일부가 이미 삭제됨: 요청={}, 실제 삭제={}", notificationIds.size(),
                notifications.size());
        }

        notificationRepository.deleteAll(notifications);
        log.debug("다건 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
    }

    public void deleteAllNotifications(String userId) {
        UserId userIdVO = new UserId(userId);
        List<Notification> notifications =
            notificationRepository.findByUserIdOrderBySentAtDesc(userIdVO);
        notificationRepository.deleteAll(notifications);
        log.debug("모든 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
    }
}
