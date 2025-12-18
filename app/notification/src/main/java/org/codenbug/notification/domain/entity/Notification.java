package org.codenbug.notification.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "user_id", nullable = false))
    private UserId userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title",
                    column = @Column(name = "title", nullable = false, length = 100)),
            @AttributeOverride(name = "content", column = @Column(name = "content", length = 500)),
            @AttributeOverride(name = "targetUrl",
                    column = @Column(name = "target_url", length = 500))})
    private NotificationContent notificationContent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean isRead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    public Notification(UserId userId, NotificationType type, NotificationContent content) {
        this.userId = Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        this.type = Objects.requireNonNull(type, "알림 유형은 필수입니다.");
        this.notificationContent = Objects.requireNonNull(content, "알림 내용은 필수입니다.");
        this.isRead = false;
        this.status = NotificationStatus.PENDING;
    }

    public static Notification createFromLegacy(String userId, NotificationType type,
            String content) {
        return new Notification(new UserId(userId), type,
                NotificationContent.fromLegacyContent(content));
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void updateStatus(NotificationStatus status) {
        this.status = Objects.requireNonNull(status, "상태는 필수입니다.");
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public boolean canRetry() {
        return this.status == NotificationStatus.FAILED;
    }

    public void retry() {
        if (!canRetry()) {
            throw new IllegalStateException("실패한 알림만 재시도할 수 있습니다.");
        }
        this.status = NotificationStatus.PENDING;
    }

    public boolean isUnread() {
        return !isRead;
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public String getTitle() {
        return notificationContent.getTitle();
    }

    public String getContent() {
        return notificationContent.getContent();
    }

    public String getTargetUrl() {
        return notificationContent.getTargetUrl();
    }

    public String getUserIdValue() {
        return userId.getValue();
    }
}
