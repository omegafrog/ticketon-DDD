package org.codenbug.notification.dto;

import java.time.LocalDateTime;

import org.codenbug.notification.domain.notification.entity.Notification;
import org.codenbug.notification.domain.notification.entity.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 정보 전송용 DTO 클래스
 * 클라이언트에 전달할 알림 정보를 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private String targetUrl;
    private LocalDateTime sentAt;
    private boolean isRead;

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .targetUrl(notification.getTargetUrl())
                .sentAt(notification.getSentAt())
                .isRead(notification.isRead())
                .build();
    }
}