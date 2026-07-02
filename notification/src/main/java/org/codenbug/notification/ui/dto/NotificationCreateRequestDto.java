package org.codenbug.notification.ui.dto;

import org.codenbug.notification.domain.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotNull(message = "알림 유형은 필수입니다")
    private NotificationType type;

    @NotBlank(message = "알림 제목은 필수입니다")
    private String title;

    @NotBlank(message = "알림 내용은 필수입니다")
    private String content;

    private String targetUrl;
}
