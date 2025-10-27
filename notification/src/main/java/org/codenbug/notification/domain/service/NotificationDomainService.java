package org.codenbug.notification.domain.service;

import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationContent;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.springframework.stereotype.Service;

@Service
public class NotificationDomainService {

    public Notification createNotification(String userId, NotificationType type, String title,
            String content, String targetUrl) {
        UserId userIdVO = new UserId(userId);
        NotificationContent notificationContent =
                new NotificationContent(title, content, targetUrl);

        return new Notification(userIdVO, type, notificationContent);
    }

    public Notification createNotification(String userId, NotificationType type, String title,
            String content) {
        return createNotification(userId, type, title, content, null);
    }

    public Notification createLegacyNotification(String userId, NotificationType type,
            String content) {
        return Notification.createFromLegacy(userId, type, content);
    }

    public boolean canMarkAsRead(Notification notification) {
        return notification.isUnread();
    }

    public boolean canRetry(Notification notification) {
        return notification.canRetry();
    }

    public void validateUserOwnership(Notification notification, String userId) {
        if (!notification.getUserIdValue().equals(userId)) {
            throw new IllegalArgumentException("해당 알림에 접근할 권한이 없습니다.");
        }
    }
}
