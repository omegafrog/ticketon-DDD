package org.codenbug.notification.application.service;

import lombok.RequiredArgsConstructor;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.service.NotificationDomainService;
import org.codenbug.notification.dto.NotificationDto;
import org.codenbug.notification.infrastructure.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

	private final NotificationRepository notificationRepository;
	private final NotificationDomainService domainService;

	public Page<NotificationDto> getNotifications(String userId, Pageable pageable) {
		UserId userIdVO = new UserId(userId);
		Page<Notification> notifications =
			notificationRepository.findByUserIdOrderBySentAtDesc(userIdVO, pageable);
		return notifications.map(NotificationDto::from);
	}

	@Transactional
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

	public Page<NotificationDto> getUnreadNotifications(String userId, Pageable pageable) {
		UserId userIdVO = new UserId(userId);
		return notificationRepository
			.findByUserIdAndIsReadFalseOrderBySentAtDesc(userIdVO, pageable)
			.map(NotificationDto::from);
	}

	public long getUnreadCount(String userId) {
		UserId userIdVO = new UserId(userId);
		return notificationRepository.countByUserIdAndIsReadFalse(userIdVO);
	}
}
