package org.codenbug.notification.dto;

import org.codenbug.notification.domain.entity.NotificationType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 생성 요청을 위한 DTO 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotNull(message = "알림 유형은 필수입니다")
    private NotificationType type;

    @NotEmpty(message = "알림 제목은 필수입니다")
    private String title;

    @NotEmpty(message = "알림 내용은 필수입니다")
    private String content;

    private String targetUrl;
}