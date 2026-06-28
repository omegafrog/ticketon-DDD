package org.codenbug.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.junit.jupiter.api.Test;

class NotificationDomainServiceTest {

    private final NotificationDomainService domainService = new NotificationDomainService();

    @Test
    void 도메인_서비스는_스프링_스테레오타입을_가지지_않는다() {
        assertThat(Arrays.stream(NotificationDomainService.class.getAnnotations())
                .map(annotation -> annotation.annotationType().getName())
                .filter(annotationName -> annotationName.startsWith("org.springframework.stereotype."))
                .toList()).isEmpty();
    }

    @Test
    void 읽지_않은_알림만_읽음_처리할_수_있다() {
        Notification notification =
                domainService.createNotification("user-1", NotificationType.SYSTEM, "제목", "내용", null);

        assertThat(domainService.canMarkAsRead(notification)).isTrue();

        notification.markAsRead();

        assertThat(domainService.canMarkAsRead(notification)).isFalse();
    }

    @Test
    void 소유자가_아니면_알림_접근을_거부한다() {
        Notification notification =
                domainService.createNotification("user-2", NotificationType.SYSTEM, "제목", "내용", null);

        assertThatThrownBy(() -> domainService.validateUserOwnership(notification, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 알림에 접근할 권한이 없습니다.");
    }
}
