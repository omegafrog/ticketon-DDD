package org.codenbug.notification.controller;

import lombok.RequiredArgsConstructor;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.notification.application.service.NotificationQueryService;
import org.codenbug.notification.dto.NotificationDto;
import org.codenbug.notification.service.NotificationEmitterService;
import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.codenbug.notification.ui.repository.NotificationViewRepository;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

	private final NotificationQueryService notificationQueryService;
	private final NotificationEmitterService emitterService;
	private final NotificationViewRepository notificationViewRepository;

	@GetMapping
	@AuthNeeded
	@RoleRequired({Role.USER})
	public RsData<Page<NotificationListProjection>> getNotifications(
			@PageableDefault(size = 10, sort = "sentAt") Pageable pageable) {
		String userId = LoggedInUserContext.get().getUserId();
		Page<NotificationListProjection> notifications =
			notificationViewRepository.findUserNotificationList(userId, pageable);
		return new RsData<>("200", "알림 목록 조회 성공", notifications);
	}

	@GetMapping("/{id}")
	public ResponseEntity<RsData<NotificationDto>> getNotificationDetail(@PathVariable Long id) {
		String userId = LoggedInUserContext.get().getUserId();
		NotificationDto notification = notificationQueryService.getNotificationById(id, userId);
		return ResponseEntity.ok(new RsData<>("200", "알림 조회 성공", notification));
	}

	@GetMapping("/unread")
	@AuthNeeded
	@RoleRequired({Role.USER})
	public RsData<Page<NotificationListProjection>> getUnreadNotifications(
			@PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
		String userId = LoggedInUserContext.get().getUserId();
		Page<NotificationListProjection> unreadPage =
			notificationViewRepository.findUserUnreadNotificationList(userId, pageable);
		return new RsData<>("200", "미읽은 알림 조회 성공", unreadPage);
	}

	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@AuthNeeded
	@RoleRequired({Role.ADMIN})
	public SseEmitter subscribeNotifications(
			@RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
		String userId = LoggedInUserContext.get().getUserId();
		return emitterService.createEmitter(userId, lastEventId);
	}

	@GetMapping("/count/unread")
	@AuthNeeded
	@RoleRequired({Role.USER})
	public ResponseEntity<RsData<Long>> getUnreadCount() {
		String userId = LoggedInUserContext.get().getUserId();
		long count = notificationViewRepository.countUnreadNotifications(userId);
		return ResponseEntity.ok(new RsData<>("200", "미읽은 알림 개수 조회 성공", count));
	}
}
