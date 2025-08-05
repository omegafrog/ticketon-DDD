package org.codenbug.notification.domain.entity;

import java.util.Objects;

public class NotificationContent {
    private final String title;
    private final String content;
    private final String targetUrl;

    public NotificationContent(String title, String content, String targetUrl) {
        this.title = validateAndNormalizeTitle(title);
        this.content = validateContent(content);
        this.targetUrl = targetUrl;
    }

    public NotificationContent(String title, String content) {
        this(title, content, null);
    }

    public static NotificationContent fromLegacyContent(String content) {
        String title = extractTitleFromContent(content);
        return new NotificationContent(title, content);
    }

    private String validateAndNormalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("알림 제목은 필수입니다.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("알림 제목은 100자를 초과할 수 없습니다.");
        }
        return trimmed;
    }

    private String validateContent(String content) {
        if (content != null && content.length() > 500) {
            throw new IllegalArgumentException("알림 내용은 500자를 초과할 수 없습니다.");
        }
        return content;
    }

    private static String extractTitleFromContent(String content) {
        if (content == null || content.isEmpty()) return "알림";
        if (content.length() <= 30) return content;
        return content.substring(0, 30) + "...";
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationContent that = (NotificationContent) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(content, that.content) &&
               Objects.equals(targetUrl, that.targetUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, content, targetUrl);
    }
}