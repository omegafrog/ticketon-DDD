package org.codenbug.notification.infra;

import java.util.List;

import org.codenbug.notification.application.port.NotificationInboxViewReader;
import org.codenbug.notification.domain.entity.QNotification;
import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class NotificationInboxViewReaderAdapter implements NotificationInboxViewReader {

    private final JPAQueryFactory queryFactory;
    private final QNotification notification = QNotification.notification;

    public NotificationInboxViewReaderAdapter(
            @Qualifier("primaryQueryFactory") JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<NotificationListProjection> findUserNotificationList(String userId,
            Pageable pageable) {
        List<NotificationListProjection> results = queryFactory
                .select(Projections.constructor(NotificationListProjection.class, notification.id,
                        notification.userId.value, notification.notificationContent.title,
                        notification.notificationContent.content, notification.type,
                        notification.isRead, notification.status.stringValue(), notification.sentAt))
                .from(notification)
                .where(notification.userId.value.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(notification.sentAt.desc())
                .fetch();

        Long total = queryFactory.select(notification.count()).from(notification)
                .where(notification.userId.value.eq(userId))
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    @Override
    public Page<NotificationListProjection> findUserUnreadNotificationList(String userId,
            Pageable pageable) {
        List<NotificationListProjection> results = queryFactory
                .select(Projections.constructor(NotificationListProjection.class, notification.id,
                        notification.userId.value, notification.notificationContent.title,
                        notification.notificationContent.content, notification.type,
                        notification.isRead, notification.status.stringValue(), notification.sentAt))
                .from(notification)
                .where(notification.userId.value.eq(userId).and(notification.isRead.isFalse()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(notification.sentAt.desc())
                .fetch();

        Long total = queryFactory.select(notification.count()).from(notification)
                .where(notification.userId.value.eq(userId).and(notification.isRead.isFalse()))
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
}
