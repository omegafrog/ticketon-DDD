package org.codenbug.notification.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.notification.application.NotificationCommandService;
import org.codenbug.notification.ui.dto.NotificationCreateRequestDto;
import org.codenbug.notification.ui.dto.NotificationDto;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합된 Notification 테스트 컨트롤러 일반 알림과 환불 알림 테스트 기능을 모두 제공
 */
@RestController
@RequestMapping("/api/v1/test-notifications")
@RequiredArgsConstructor
public class TestNotificationController {

    private final NotificationCommandService notificationCommandService;

    @PostMapping
    @AuthNeeded
    @RoleRequired({Role.ADMIN, Role.MANAGER})
    public ResponseEntity<RsData<NotificationDto>> createTestNotification(
            @RequestBody @Valid NotificationCreateRequestDto requestDto) {
        NotificationDto createdNotification = notificationCommandService.createNotification(
                requestDto.getUserId(),
                requestDto.getType(),
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getTargetUrl());
        return ResponseEntity.status(201)
                .body(new RsData<>("201", "알림 생성 성공", createdNotification));
    }
}
