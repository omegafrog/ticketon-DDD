package org.codenbug.notification.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.codenbug.common.Role;
import org.codenbug.notification.application.NotificationCommandService;
import org.codenbug.notification.domain.entity.NotificationSelection;
import org.codenbug.notification.ui.dto.NotificationDeleteRequestDto;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class NotificationCommandControllerDeleteTest {

    private final NotificationCommandService notificationCommandService =
            org.mockito.Mockito.mock(NotificationCommandService.class);
    private final NotificationCommandController controller =
            new NotificationCommandController(notificationCommandService);
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void delete_엔드포인트는_인증과_ADMIN_권한을_요구한다() throws Exception {
        assertAdminProtected("deleteNotification", Long.class);
        assertAdminProtected("batchDeleteNotifications", NotificationDeleteRequestDto.class);
        assertAdminProtected("deleteAllNotifications");
    }

    @Test
    void 미인증_삭제요청은_context_없으면_거절된다() {
        assertThatThrownBy(() -> controller.deleteNotification(1L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void authenticated_batch_delete는_정규화된_selection을_서비스에_전달한다() throws Exception {
        try (LoggedInUserContext ignored = LoggedInUserContext
                .open(new UserSecurityToken("user-1", "user-1@ticketon.site", Role.ADMIN))) {
            controller.batchDeleteNotifications(
                    new NotificationDeleteRequestDto(List.of(3L, 1L, 3L, 2L)));
        }

        verify(notificationCommandService).deleteSelectedNotifications(
                eq(NotificationSelection.from(List.of(3L, 1L, 3L, 2L))), eq("user-1"));
    }

    @Test
    void dto_validation은_empty와_null_element를_거절한다() {
        Set<ConstraintViolation<NotificationDeleteRequestDto>> emptyViolations =
                validator.validate(new NotificationDeleteRequestDto(List.of()));
        Set<ConstraintViolation<NotificationDeleteRequestDto>> nullElementViolations =
                validator.validate(new NotificationDeleteRequestDto(Arrays.asList(1L, null)));

        assertThat(emptyViolations).isNotEmpty();
        assertThat(nullElementViolations).isNotEmpty();
    }

    private void assertAdminProtected(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = NotificationCommandController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(AuthNeeded.class)).isNotNull();
        RoleRequired roleRequired = method.getAnnotation(RoleRequired.class);
        assertThat(roleRequired).isNotNull();
        assertThat(roleRequired.value()).containsExactly(Role.ADMIN);
    }
}
