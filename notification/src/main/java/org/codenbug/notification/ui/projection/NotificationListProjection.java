package org.codenbug.notification.ui.projection;

import java.time.LocalDateTime;

/**
 * 알림 리스트 조회용 Projection
 * 뷰 전용으로 필요한 필드만 포함하여 N+1 문제를 방지
 */
public class NotificationListProjection {
    private final Long id;
    private final String userId;
    private final String title;
    private final String content;
    private final String type;
    private final boolean isRead;
    private final String status;
    private final LocalDateTime sentAt;

    public NotificationListProjection(Long id, String userId, String title, String content,
                                    String type, boolean isRead, String status,
                                    LocalDateTime sentAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.isRead = isRead;
        this.status = status;
        this.sentAt = sentAt;
    }
    
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public String getStatus() { return status; }
    public LocalDateTime getSentAt() { return sentAt; }
}