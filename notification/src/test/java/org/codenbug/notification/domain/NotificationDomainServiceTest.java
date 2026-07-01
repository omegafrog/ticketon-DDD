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
        assertThat(domainService.markAsReadIfUnread(notification)).isTrue();

        assertThat(domainService.canMarkAsRead(notification)).isFalse();
        assertThat(domainService.markAsReadIfUnread(notification)).isFalse();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void 소유자가_아니면_알림_접근을_거부한다() {
        Notification notification =
                domainService.createNotification("user-2", NotificationType.SYSTEM, "제목", "내용", null);

        assertThatThrownBy(() -> domainService.validateUserOwnership(notification, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 알림에 접근할 권한이 없습니다.");
    }

    @Test
    void 생성계약은_legacy와_신규_경로_모두_유지한다() {
        Notification created = domainService.createNotification(" user-1 ", NotificationType.SYSTEM,
                " 제목 ", "내용", "/target");
        Notification legacy = domainService.createLegacyNotification("user-2",
                NotificationType.PAYMENT, "legacy-content");

        assertThat(created.getUserIdValue()).isEqualTo("user-1");
        assertThat(created.getTitle()).isEqualTo("제목");
        assertThat(created.getContent()).isEqualTo("내용");
        assertThat(created.isUnread()).isTrue();
        assertThat(legacy.getUserIdValue()).isEqualTo("user-2");
        assertThat(legacy.getContent()).isEqualTo("legacy-content");
        assertThat(legacy.isUnread()).isTrue();
    }

    @Test
    void blank_리시피언트는_생성_전에_거절한다() {
        assertThatThrownBy(() -> domainService.createNotification("   ", NotificationType.SYSTEM,
                "제목", "내용", null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다.");
    }

    @Test
    void blank_제목은_생성_전에_거절한다() {
        assertThatThrownBy(() -> domainService.createNotification("user-1", NotificationType.SYSTEM,
                "   ", "내용", null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 제목은 필수입니다.");
    }

    @Test
    void blank_내용은_생성_전에_거절한다() {
        assertThatThrownBy(() -> domainService.createNotification("user-1", NotificationType.SYSTEM,
                "제목", "   ", null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 내용은 필수입니다.");
    }

    @Test
    void targetUrl은_optional이며_blank면_null로_정규화한다() {
        Notification notification = domainService.createNotification("user-1", NotificationType.SYSTEM,
                "제목", "내용", "   ");

        assertThat(notification.getTargetUrl()).isNull();
        assertThat(notification.isUnread()).isTrue();
    }
}
