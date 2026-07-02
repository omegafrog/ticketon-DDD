package org.codenbug.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationSelection;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.junit.jupiter.api.Test;

class NotificationDeletionPolicyTest {

    private final NotificationDomainService domainService = new NotificationDomainService();
    private final NotificationDeletionPolicy notificationDeletionPolicy =
            new NotificationDeletionPolicy();

    @Test
    void duplicate_id는_정규화되어_중복없이_평가된다() {
        NotificationSelection selection = NotificationSelection.from(List.of(1L, 2L, 1L, 2L));
        Notification first = notificationWithId(1L, "user-1");
        Notification second = notificationWithId(2L, "user-1");

        NotificationDeletionPolicy.SelectionDeletionDecision decision =
                notificationDeletionPolicy.evaluate(new UserId("user-1"), selection,
                        List.of(first, second));

        assertThat(selection.getNotificationIds()).containsExactly(1L, 2L);
        assertThat(decision.isRejected()).isFalse();
        assertThat(decision.deletedCount()).isEqualTo(2);
    }

    @Test
    void existing_foreign_owned가_하나라도_있으면_전체_거절한다() {
        NotificationSelection selection = NotificationSelection.from(List.of(1L, 2L));
        Notification owned = notificationWithId(1L, "user-1");
        Notification foreignOwned = notificationWithId(2L, "user-2");

        NotificationDeletionPolicy.SelectionDeletionDecision decision =
                notificationDeletionPolicy.evaluate(new UserId("user-1"), selection,
                        List.of(owned, foreignOwned));

        assertThat(decision.isRejected()).isTrue();
        assertThat(decision.deletedCount()).isZero();
        assertThat(decision.rejectionReasonCategory()).isEqualTo("FOREIGN_OWNED");
    }

    @Test
    void missing_owned는_무시하고_existing_owned만_삭제대상으로_남긴다() {
        NotificationSelection selection = NotificationSelection.from(List.of(1L, 2L, 3L));
        Notification owned = notificationWithId(1L, "user-1");

        NotificationDeletionPolicy.SelectionDeletionDecision decision =
                notificationDeletionPolicy.evaluate(new UserId("user-1"), selection,
                        List.of(owned));

        assertThat(decision.isRejected()).isFalse();
        assertThat(decision.requestedCount()).isEqualTo(3);
        assertThat(decision.deletableNotifications()).containsExactly(owned);
    }

    @Test
    void existing_owned가_하나도_없어도_정상_성공한다() {
        NotificationSelection selection = NotificationSelection.from(List.of(7L, 8L));

        NotificationDeletionPolicy.SelectionDeletionDecision decision =
                notificationDeletionPolicy.evaluate(new UserId("user-1"), selection, List.of());

        assertThat(decision.isRejected()).isFalse();
        assertThat(decision.deletedCount()).isZero();
        assertThat(decision.rejectionReasonCategory()).isEqualTo("NONE");
    }

    private Notification notificationWithId(Long id, String userId) {
        Notification notification =
                domainService.createNotification(userId, NotificationType.SYSTEM, "제목", "내용", "/target");
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
