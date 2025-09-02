package org.codenbug.notification.ui.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.QNotification;
import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class NotificationViewRepositoryImpl implements NotificationViewRepository {
    
    private final JPAQueryFactory queryFactory;
    private final QNotification notification = QNotification.notification;
    
    public NotificationViewRepositoryImpl(@Qualifier("primaryQueryFactory") JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }
    
    @Override
    public Page<NotificationListProjection> findUserNotificationList(String userId, Pageable pageable) {
        List<NotificationListProjection> results = queryFactory
            .select(Projections.constructor(NotificationListProjection.class,
                notification.id,
                notification.userId.value,
                notification.notificationContent.title,
                notification.notificationContent.content,
                notification.type,
                notification.isRead,
                notification.status.stringValue(),
                notification.sentAt
            ))
            .from(notification)
            .where(notification.userId.value.eq(userId))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(notification.sentAt.desc())
            .fetch();
        
        Long total = queryFactory
            .select(notification.count())
            .from(notification)
            .where(notification.userId.value.eq(userId))
            .fetchOne();
        
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
    
    @Override
    public Page<NotificationListProjection> findUserUnreadNotificationList(String userId, Pageable pageable) {
        List<NotificationListProjection> results = queryFactory
            .select(Projections.constructor(NotificationListProjection.class,
                notification.id,
                notification.userId.value,
                notification.notificationContent.title,
                notification.notificationContent.content,
                notification.type,
                notification.isRead,
                notification.status.stringValue(),
                notification.sentAt
            ))
            .from(notification)
            .where(notification.userId.value.eq(userId)
                .and(notification.isRead.isFalse()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(notification.sentAt.desc())
            .fetch();
        
        Long total = queryFactory
            .select(notification.count())
            .from(notification)
            .where(notification.userId.value.eq(userId)
                .and(notification.isRead.isFalse()))
            .fetchOne();
        
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    @Override
    public Page<NotificationListProjection> findUserNotificationListByType(String userId, String type,
        Pageable pageable) {
        List<NotificationListProjection> results = queryFactory
            .select(Projections.constructor(NotificationListProjection.class,
                notification.id,
                notification.userId.value,
                notification.notificationContent.title,
                notification.notificationContent.content,
                notification.type,
                notification.isRead,
                notification.status.stringValue(),
                notification.sentAt
            ))
            .from(notification)
            .where(notification.userId.value.eq(userId)
                .and(StringUtils.hasText(type) ? notification.type.eq(NotificationType.valueOf(type)) : null))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(notification.sentAt.desc())
            .fetch();

        Long total = queryFactory
            .select(notification.count())
            .from(notification)
            .where(notification.userId.value.eq(userId)
                .and(StringUtils.hasText(type) ? notification.type.eq(NotificationType.valueOf(type)) : null))
            .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
    
    @Override
    public List<NotificationListProjection> findUserNotificationListWithCursor(String userId, Long cursor, int size) {
        return queryFactory
            .select(Projections.constructor(NotificationListProjection.class,
                notification.id,
                notification.userId.value,
                notification.notificationContent.title,
                notification.notificationContent.content,
                notification.type,
                notification.isRead,
                notification.status.stringValue(),
                notification.sentAt
            ))
            .from(notification)
            .where(notification.userId.value.eq(userId)
                .and(cursor != null ? notification.id.gt(cursor) : null))
            .orderBy(notification.id.asc())
            .limit(size)
            .fetch();
    }
    
    @Override
    public long countUnreadNotifications(String userId) {
        Long count = queryFactory
            .select(notification.count())
            .from(notification)
            .where(notification.userId.value.eq(userId)
                .and(notification.isRead.isFalse()))
            .fetchOne();
        
        return count != null ? count : 0;
    }
}