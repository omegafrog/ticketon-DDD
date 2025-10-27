package org.codenbug.notification.ui.repository;

import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 알림 뷰 조회 전용 Repository Projection을 사용하여 N+1 문제 없이 최적화된 쿼리 수행
 */
public interface NotificationViewRepository {

    /**
     * 사용자별 알림 리스트 조회
     */
    Page<NotificationListProjection> findUserNotificationList(String userId, Pageable pageable);

    /**
     * 사용자별 미읽은 알림 리스트 조회
     */
    Page<NotificationListProjection> findUserUnreadNotificationList(String userId,
            Pageable pageable);

    /**
     * 사용자별 특정 타입 알림 리스트 조회
     */
    Page<NotificationListProjection> findUserNotificationListByType(String userId, String type,
            Pageable pageable);

    /**
     * 커서 기반 페이징으로 알림 리스트 조회
     */
    List<NotificationListProjection> findUserNotificationListWithCursor(String userId, Long cursor,
            int size);

    /**
     * 사용자별 읽지 않은 알림 수 조회
     */
    long countUnreadNotifications(String userId);
}
