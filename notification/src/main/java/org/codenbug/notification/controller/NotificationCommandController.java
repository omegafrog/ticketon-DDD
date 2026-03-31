package org.codenbug.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.notification.application.service.NotificationCommandService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.dto.NotificationCreateRequestDto;
import org.codenbug.notification.dto.NotificationDeleteRequestDto;
import org.codenbug.notification.dto.NotificationDto;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationCommandController {

	private final NotificationCommandService notificationCommandService;

	@PostMapping
	@AuthNeeded
	@RoleRequired({Role.ADMIN, Role.MANAGER})
	public ResponseEntity<RsData<NotificationDto>> createNotification(
			@RequestBody @Valid NotificationCreateRequestDto requestDto) {
		NotificationDto createdNotification = notificationCommandService.createNotification(
			requestDto.getUserId(),
			requestDto.getType(),
			requestDto.getTitle(),
			requestDto.getContent(),
			requestDto.getTargetUrl()
		);
		return ResponseEntity.status(201)
			.body(new RsData<>("201", "알림 생성 성공", createdNotification));
	}

	@DeleteMapping("/{id}")
	@AuthNeeded
	@RoleRequired({Role.ADMIN})
	public ResponseEntity<RsData<Void>> deleteNotification(@PathVariable Long id) {
		String userId = LoggedInUserContext.get().getUserId();
		notificationCommandService.deleteNotification(id, userId);
		return ResponseEntity.ok(new RsData<>("200", "알림 삭제 성공", null));
	}

	@PostMapping("/batch-delete")
	@AuthNeeded
	@RoleRequired({Role.ADMIN})
	public ResponseEntity<RsData<Void>> batchDeleteNotifications(
			@Valid @RequestBody NotificationDeleteRequestDto request) {
		String userId = LoggedInUserContext.get().getUserId();
		notificationCommandService.deleteNotifications(request.getNotificationIds(), userId);
		return ResponseEntity.ok(new RsData<>("200", "알림 삭제 성공", null));
	}

	@DeleteMapping("/all")
	@AuthNeeded
	@RoleRequired({Role.ADMIN})
	public ResponseEntity<RsData<Void>> deleteAllNotifications() {
		String userId = LoggedInUserContext.get().getUserId();
		notificationCommandService.deleteAllNotifications(userId);
		return ResponseEntity.ok(new RsData<>("200", "모든 알림 삭제 성공", null));
	}

	@PostMapping("/test")
	public ResponseEntity<RsData<NotificationDto>> createTestNotification(
			@RequestParam String userId,
			@RequestParam(defaultValue = "SYSTEM") NotificationType type,
			@RequestParam(defaultValue = "테스트 알림") String title,
			@RequestParam(defaultValue = "테스트 알림 내용입니다.") String content) {
		NotificationDto notification =
			notificationCommandService.createNotification(userId, type, title, content);
		return ResponseEntity.ok(new RsData<>("200", "테스트 알림 생성 성공", notification));
	}
}
