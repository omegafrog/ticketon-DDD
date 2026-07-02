package org.codenbug.notification.application.port;

import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationInboxViewReader {

    Page<NotificationListProjection> findUserNotificationList(String userId, Pageable pageable);

    Page<NotificationListProjection> findUserUnreadNotificationList(String userId,
            Pageable pageable);
}
