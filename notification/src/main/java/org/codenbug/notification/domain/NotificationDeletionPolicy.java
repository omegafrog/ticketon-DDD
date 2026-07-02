package org.codenbug.notification.domain;

import java.util.List;

import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationSelection;
import org.codenbug.notification.domain.entity.UserId;

public class NotificationDeletionPolicy {

    public SelectionDeletionDecision evaluate(UserId requesterId, NotificationSelection selection,
            List<Notification> existingNotifications) {
        boolean hasForeignOwned = existingNotifications.stream()
                .anyMatch(notification -> !notification.isOwnedBy(requesterId));

        if (hasForeignOwned) {
            return SelectionDeletionDecision.rejected(selection.size(), "FOREIGN_OWNED");
        }

        List<Notification> deletableNotifications = existingNotifications.stream()
                .filter(notification -> notification.isOwnedBy(requesterId))
                .toList();
        return SelectionDeletionDecision.accepted(selection.size(), deletableNotifications);
    }

    public record SelectionDeletionDecision(
            boolean isRejected,
            int requestedCount,
            List<Notification> deletableNotifications,
            String rejectionReasonCategory
    ) {

        private static SelectionDeletionDecision accepted(int requestedCount,
                List<Notification> deletableNotifications) {
            return new SelectionDeletionDecision(false, requestedCount,
                    List.copyOf(deletableNotifications), "NONE");
        }

        private static SelectionDeletionDecision rejected(int requestedCount,
                String rejectionReasonCategory) {
            return new SelectionDeletionDecision(true, requestedCount, List.of(),
                    rejectionReasonCategory);
        }

        public int deletedCount() {
            return deletableNotifications.size();
        }
    }
}
