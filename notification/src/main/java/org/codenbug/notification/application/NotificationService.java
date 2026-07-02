package org.codenbug.notification.application;

import java.util.List;

import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationStatus;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.NotificationDomainService;
import org.codenbug.notification.ui.dto.NotificationDto;
import org.codenbug.notification.ui.dto.NotificationEventDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 관련 비즈니스 로직을 처리하는 서비스 단순히 알림 생성과 전송만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationStore notificationStore;
    private final NotificationDomainService domainService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 특정 사용자의 알림 목록을 페이지네이션하여 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징된 알림 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(String userId, Pageable pageable) {
        UserId userIdVO = new UserId(userId);
        Page<Notification> notifications =
                notificationStore.findByUserIdOrderBySentAtDesc(userIdVO, pageable);
        return notifications.map(NotificationDto::from);
    }

    /**
     * 특정 알림의 상세 정보를 조회하고 읽음 상태로 업데이트
     *
     * @param notificationId 조회할 알림 ID
     * @param userId 현재 인증된 사용자 ID
     * @return 알림 상세 정보 DTO
     * @throws IllegalArgumentException 알림을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public NotificationDto getNotificationById(Long notificationId, String userId) {
        // 알림 조회
        Notification notification = notificationStore.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));

        domainService.validateUserOwnership(notification, userId);

        // 읽음 상태 업데이트
        if (!notification.isRead()) {
            notification.markAsRead();
            notificationStore.save(notification);
        }

        return NotificationDto.from(notification);
    }

    /**
     * 새로운 알림을 생성합니다 트랜잭션 동기화를 위해 이벤트 발행 방식 사용
     * 
     * @param userId 알림을 받을 사용자 ID
     * @param type 알림 유형
     * @param title 알림 제목
     * @param content 알림 내용
     * @return 생성된 알림 DTO
     */
    @Transactional
    public NotificationDto createNotification(String userId, NotificationType type, String title,
            String content, String targetUrl) {
        log.debug("알림 생성 시작: userId={}, type={}, title={}", userId, type, title);

        // 사용자 검증 생략 - 알림 모듈은 단순히 알림 생성만 담당

        // 알림 엔티티 생성
        Notification notification =
                domainService.createNotification(userId, type, title, content, targetUrl);

        // 저장
        Notification savedNotification = notificationStore.save(notification);
        log.debug("알림 저장 완료: notificationId={}", savedNotification.getId());

        // DTO로 변환
        NotificationDto notificationDto = NotificationDto.from(savedNotification);

        // 확장된 이벤트 DTO 생성 및 발행
        NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
        eventPublisher.publishEvent(eventDto);

        log.debug("알림 이벤트 발행 완료: notificationId={}", savedNotification.getId());

        return notificationDto;
    }

    // 오버로드된 메서드 - targetUrl 없이 호출할 경우
    @Transactional
    public NotificationDto createNotification(String userId, NotificationType type, String title,
            String content) {
        return createNotification(userId, type, title, content, null);
    }

    /**
     * 하위 호환성을 위한 메서드 (기존 코드)
     */
    @Transactional
    public NotificationDto createNotification(String userId, NotificationType type,
            String content) {
        // 도메인 서비스를 통한 레거시 알림 생성
        Notification notification = domainService.createLegacyNotification(userId, type, content);

        // 저장
        Notification savedNotification = notificationStore.save(notification);
        log.debug("레거시 알림 저장 완료: notificationId={}", savedNotification.getId());

        // DTO로 변환
        NotificationDto notificationDto = NotificationDto.from(savedNotification);

        // 확장된 이벤트 DTO 생성 및 발행
        NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
        eventPublisher.publishEvent(eventDto);

        log.debug("레거시 알림 이벤트 발행 완료: notificationId={}", savedNotification.getId());

        return notificationDto;
    }

    /**
     * 사용자의 미읽은 알림 목록을 페이지네이션하여 조회합니다
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 미읽은 알림 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUnreadNotifications(String userId, Pageable pageable) {
        UserId userIdVO = new UserId(userId);
        return notificationStore
                .findByUserIdAndIsReadFalseOrderBySentAtDesc(userIdVO, pageable)
                .map(NotificationDto::from);
    }

    /**
     * 알림 상태별 카운트 조회
     *
     * @param userId 사용자 ID
     * @return 상태별 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        UserId userIdVO = new UserId(userId);
        return notificationStore.countByUserIdAndIsReadFalse(userIdVO);
    }

    /**
     * 실패한 알림 재전송 시도
     *
     * @param notificationId 알림 ID
     * @return 재전송 성공 여부
     */
    @Transactional
    public boolean retryFailedNotification(Long notificationId) {
        Notification notification = notificationStore.findById(notificationId).orElse(null);

        if (notification == null || notification.getStatus() != NotificationStatus.FAILED) {
            return false;
        }

        // 상태를 PENDING으로 변경
        notification.updateStatus(NotificationStatus.PENDING);
        notificationStore.save(notification);

        // 이벤트 재발행
        NotificationEventDto eventDto = NotificationEventDto.from(notification);
        eventPublisher.publishEvent(eventDto);

        return true;
    }

    /**
     * 단일 알림을 삭제합니다
     *
     * @param notificationId 삭제할 알림 ID
     * @param userId 현재 인증된 사용자 ID
     * @throws IllegalArgumentException 알림이 존재하지 않거나 권한이 없는 경우
     */
    @Transactional
    public void deleteNotification(Long notificationId, String userId) {
        Notification notification = notificationStore.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));

        domainService.validateUserOwnership(notification, userId);

        notificationStore.delete(notification);
        log.debug("알림 삭제 완료: notificationId={}, userId={}", notificationId, userId);
    }

    /**
     * 여러 알림을 삭제합니다(멱등성 보장)
     *
     * @param notificationIds 삭제할 알림 ID 목록
     * @param userId 현재 인증된 사용자 ID
     */
    @Transactional
    public void deleteNotifications(List<Long> notificationIds, String userId) {
        // 사용자의 알림 중 요청된 ID 목록에 해당하는 알림만 조회
        UserId userIdVO = new UserId(userId);
        List<Notification> notifications =
                notificationStore.findAllByUserIdAndIdIn(userIdVO, notificationIds);

        // 찾은 알림 개수와 요청 개수의 차이 로깅
        if (notifications.size() < notificationIds.size()) {
            log.info("요청된 알림 중 일부가 이미 삭제됨: 요청={}, 실제 삭제={}", notificationIds.size(),
                    notifications.size());
        }

        // 존재하는 알림만 삭제
        notificationStore.deleteAll(notifications);
        log.debug("다건 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
    }

    /**
     * 사용자의 모든 알림을 삭제합니다
     *
     * @param userId 현재 인증된, 사용자 ID
     */
    @Transactional
    public void deleteAllNotifications(String userId) {
        UserId userIdVO = new UserId(userId);
        List<Notification> notifications =
                notificationStore.findByUserIdOrderBySentAtDesc(userIdVO);
        notificationStore.deleteAll(notifications);
        log.debug("모든 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
    }
}
