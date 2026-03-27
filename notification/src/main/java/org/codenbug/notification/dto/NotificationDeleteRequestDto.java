package org.codenbug.notification.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다건 알림 삭제 요청을 위한 DTO 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeleteRequestDto {
	@NotEmpty(message = "삭제할 알림 ID 목록은 비어 있을 수 없습니다")
    private List<Long> notificationIds;
}
