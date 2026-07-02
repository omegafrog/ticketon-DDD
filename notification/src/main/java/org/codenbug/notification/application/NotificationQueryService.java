package org.codenbug.notification.application;

import lombok.RequiredArgsConstructor;
import org.codenbug.notification.application.port.NotificationInboxViewReader;
import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.UserId;
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

	public Page<NotificationListProjection> getNotifications(String userId, Pageable pageable) {
		return notificationInboxViewReader.findUserNotificationList(userId, pageable);
	}

	@Transactional
	public Notification getNotificationById(Long notificationId, String userId) {
		UserId recipientUserId = new UserId(userId);
		Notification notification = notificationStore.findByIdAndUserId(notificationId, recipientUserId)
			.orElseGet(() -> loadOwnedNotificationOrThrow(notificationId, recipientUserId));

		if (notification.markAsReadIfUnread()) {
			notificationStore.save(notification);
		}

		return notification;
	}

	public Page<NotificationListProjection> getUnreadNotifications(String userId, Pageable pageable) {
		return notificationInboxViewReader.findUserUnreadNotificationList(userId, pageable);
	}

	public long getUnreadCount(String userId) {
		UserId userIdVO = new UserId(userId);
		return notificationStore.countByUserIdAndIsReadFalse(userIdVO);
	}

	private Notification loadOwnedNotificationOrThrow(Long notificationId, UserId userId) {
		Notification notification = notificationStore.findById(notificationId)
			.orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));
		if (!notification.isOwnedBy(userId.getValue())) {
			throw new IllegalArgumentException("해당 알림에 접근할 권한이 없습니다.");
		}
		return notification;
	}
}
