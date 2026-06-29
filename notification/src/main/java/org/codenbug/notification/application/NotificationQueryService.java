package org.codenbug.notification.application;

import lombok.RequiredArgsConstructor;
import org.codenbug.notification.application.port.NotificationInboxViewReader;
import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.NotificationDomainService;
import org.codenbug.notification.ui.dto.NotificationDto;
import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

	private final NotificationStore notificationStore;
	private final NotificationInboxViewReader notificationInboxViewReader;
	private final NotificationDomainService domainService;

	public Page<NotificationListProjection> getNotifications(String userId, Pageable pageable) {
		return notificationInboxViewReader.findUserNotificationList(userId, pageable);
	}

	@Transactional
	public NotificationDto getNotificationById(Long notificationId, String userId) {
		Notification notification = notificationStore.findById(notificationId)
			.orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));

		domainService.validateUserOwnership(notification, userId);

		if (domainService.canMarkAsRead(notification)) {
			notification.markAsRead();
			notificationStore.save(notification);
		}

		return NotificationDto.from(notification);
	}

	public Page<NotificationListProjection> getUnreadNotifications(String userId, Pageable pageable) {
		return notificationInboxViewReader.findUserUnreadNotificationList(userId, pageable);
	}

	public long getUnreadCount(String userId) {
		UserId userIdVO = new UserId(userId);
		return notificationStore.countByUserIdAndIsReadFalse(userIdVO);
	}
}
