package org.codenbug.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.service.NotificationDomainService;
import org.codenbug.notification.dto.NotificationDto;
import org.codenbug.notification.dto.NotificationEventDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

	private final NotificationStore notificationStore;
	private final NotificationDomainService domainService;
	private final ApplicationEventPublisher eventPublisher;

	public NotificationDto createNotification(String userId, NotificationType type, String title,
			String content, String targetUrl) {
		log.debug("알림 생성 시작: userId={}, type={}, title={}", userId, type, title);
		Notification notification =
			domainService.createNotification(userId, type, title, content, targetUrl);
		Notification savedNotification = notificationStore.save(notification);

		NotificationDto notificationDto = NotificationDto.from(savedNotification);
		NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
		eventPublisher.publishEvent(eventDto);

		log.debug("알림 이벤트 발행 완료: notificationId={}", savedNotification.getId());
		return notificationDto;
	}

	public NotificationDto createNotification(String userId, NotificationType type, String title,
			String content) {
		return createNotification(userId, type, title, content, null);
	}

	public NotificationDto createLegacyNotification(String userId, NotificationType type, String content) {
		Notification notification = domainService.createLegacyNotification(userId, type, content);
		Notification savedNotification = notificationStore.save(notification);

		NotificationDto notificationDto = NotificationDto.from(savedNotification);
		NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
		eventPublisher.publishEvent(eventDto);

		return notificationDto;
	}

	public boolean retryFailedNotification(Long notificationId) {
		Notification notification = notificationStore.findById(notificationId).orElse(null);
		if (notification == null || !domainService.canRetry(notification)) {
			return false;
		}

		notification.retry();
		notificationStore.save(notification);
		NotificationEventDto eventDto = NotificationEventDto.from(notification);
		eventPublisher.publishEvent(eventDto);

		return true;
	}

	public void deleteNotification(Long notificationId, String userId) {
		Notification notification = notificationStore.findById(notificationId)
			.orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));
		domainService.validateUserOwnership(notification, userId);
		notificationStore.delete(notification);
		log.debug("알림 삭제 완료: notificationId={}, userId={}", notificationId, userId);
	}

	public void deleteNotifications(List<Long> notificationIds, String userId) {
		UserId userIdVO = new UserId(userId);
		List<Notification> notifications =
			notificationStore.findAllByUserIdAndIdIn(userIdVO, notificationIds);

		if (notifications.size() < notificationIds.size()) {
			log.info("요청된 알림 중 일부가 이미 삭제됨: 요청={}, 실제 삭제={}", notificationIds.size(),
				notifications.size());
		}

		notificationStore.deleteAll(notifications);
		log.debug("다건 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
	}

	public void deleteAllNotifications(String userId) {
		UserId userIdVO = new UserId(userId);
		List<Notification> notifications =
			notificationStore.findByUserIdOrderBySentAtDesc(userIdVO);
		notificationStore.deleteAll(notifications);
		log.debug("모든 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
	}
}
