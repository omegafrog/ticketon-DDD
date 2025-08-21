package org.codenbug.notification.controller;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.notification.application.service.NotificationApplicationService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.dto.NotificationCreateRequestDto;
import org.codenbug.notification.dto.NotificationDeleteRequestDto;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 API 엔드포인트를 제공하는 컨트롤러
 * App 모듈 내에서 동작
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;
    private final NotificationEmitterService emitterService;
    private final NotificationViewRepository notificationViewRepository;

    /**
     * 알림 목록 조회 API (최적화된 Projection 사용)
     */
    @GetMapping
    @AuthNeeded
    @RoleRequired({Role.USER})
    public RsData<Page<NotificationListProjection>> getNotifications(
            @PageableDefault(size = 10, sort = "sentAt") Pageable pageable) {

        String userId = LoggedInUserContext.get().getUserId();
        // 최적화된 Projection 조회로 N+1 문제 해결
        Page<NotificationListProjection> notifications = notificationViewRepository.findUserNotificationList(userId, pageable);
        return new RsData<>("200-SUCCESS", "알림 목록 조회 성공", notifications);
    }

    /**
     * 알림 상세 조회 API
     */
    @GetMapping("/{id}")
    public ResponseEntity<RsData<NotificationDto>> getNotificationDetail(@PathVariable Long id) {
        String userId = LoggedInUserContext.get().getUserId();

        NotificationDto notification = notificationApplicationService.getNotificationById(id, userId);
        return ResponseEntity.ok(new RsData<>("200-SUCCESS", "알림 조회 성공", notification));
    }

    /**
     * 알림 생성 API (관리자/시스템 전용)
     */
    @PostMapping
    @AuthNeeded
    @RoleRequired({Role.ADMIN, Role.MANAGER})
    public ResponseEntity<RsData<NotificationDto>> createNotification(
            @RequestBody @Valid NotificationCreateRequestDto requestDto) {

        NotificationDto createdNotification = notificationApplicationService.createNotification(
                requestDto.getUserId(),
                requestDto.getType(),
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getTargetUrl()
        );

        return ResponseEntity.ok(new RsData<>("200-SUCCESS", "알림 생성 성공", createdNotification));
    }

    /**
     * 미읽은 알림 조회 API (최적화된 Projection 사용)
     */
    @GetMapping("/unread")
    @AuthNeeded
    @RoleRequired({Role.USER})
    public RsData<Page<NotificationListProjection>> getUnreadNotifications(
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String userId = LoggedInUserContext.get().getUserId();
        // 최적화된 Projection 조회로 N+1 문제 해결
        Page<NotificationListProjection> unreadPage = notificationViewRepository.findUserUnreadNotificationList(userId, pageable);
        return new RsData<>("200-SUCCESS", "미읽은 알림 조회 성공", unreadPage);
    }

    /**
     * 알림 구독 API (SSE)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthNeeded
    @RoleRequired({Role.ADMIN})
    public SseEmitter subscribeNotifications(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        String userId = LoggedInUserContext.get().getUserId();

        return emitterService.createEmitter(userId, lastEventId);
    }

    /**
     * 단일 알림 삭제 API
     */
    @DeleteMapping("/{id}")
    @AuthNeeded
    @RoleRequired({Role.ADMIN})
    public ResponseEntity<RsData<Void>> deleteNotification(@PathVariable Long id) {
        String userId = LoggedInUserContext.get().getUserId();

        notificationApplicationService.deleteNotification(id, userId);
        return ResponseEntity.ok(new RsData<>("200-SUCCESS", "알림 삭제 성공", null));
    }

    /**
     * 다건 알림 삭제 API
     */
    @PostMapping("/batch-delete")
    @AuthNeeded
    @RoleRequired({Role.ADMIN})
    public ResponseEntity<RsData<Void>> batchDeleteNotifications(@RequestBody NotificationDeleteRequestDto request) {
                String userId = LoggedInUserContext.get().getUserId();

        notificationApplicationService.deleteNotifications(request.getNotificationIds(), userId);
        return ResponseEntity.ok(new RsData<>("200", "알림 삭제 성공", null));
    }

    /**
     * 모든 알림 삭제 API
     */
    @DeleteMapping("/all")
    @AuthNeeded
    @RoleRequired({Role.ADMIN})
    public ResponseEntity<RsData<Void>> deleteAllNotifications() {
                String userId = LoggedInUserContext.get().getUserId();

        notificationApplicationService.deleteAllNotifications(userId);
        return ResponseEntity.ok(new RsData<>("200-SUCCESS", "모든 알림 삭제 성공", null));
    }

    /**
     * 알림 개수 조회 API (최적화된 Projection 사용)
     */
    @GetMapping("/count/unread")
    @AuthNeeded
    @RoleRequired({Role.USER})
    public ResponseEntity<RsData<Long>> getUnreadCount() {
        String userId = LoggedInUserContext.get().getUserId();

        // 최적화된 COUNT 쿼리 사용
        long count = notificationViewRepository.countUnreadNotifications(userId);
        return ResponseEntity.ok(new RsData<>("200-SUCCESS", "미읽은 알림 개수 조회 성공", count));
    }

    /**
     * 테스트용 알림 생성 API
     */
    @PostMapping("/test")
    public ResponseEntity<RsData<NotificationDto>> createTestNotification(
            @RequestParam String userId,
            @RequestParam(defaultValue = "SYSTEM") NotificationType type,
            @RequestParam(defaultValue = "테스트 알림") String title,
            @RequestParam(defaultValue = "테스트 알림 내용입니다.") String content) {

        NotificationDto notification = notificationApplicationService.createNotification(
                userId, type, title, content);

        return ResponseEntity.ok(new RsData<>("200-SUCCESS", "테스트 알림 생성 성공", notification));
    }
}