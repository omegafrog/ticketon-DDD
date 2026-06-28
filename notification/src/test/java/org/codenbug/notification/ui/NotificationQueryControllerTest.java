package org.codenbug.notification.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.codenbug.common.Role;
import org.codenbug.notification.application.NotificationQueryService;
import org.codenbug.notification.application.NotificationEmitterService;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class NotificationQueryControllerTest {

    private final NotificationQueryService notificationQueryService =
            org.mockito.Mockito.mock(NotificationQueryService.class);
    private final NotificationEmitterService notificationEmitterService =
            org.mockito.Mockito.mock(NotificationEmitterService.class);
    private final NotificationQueryController controller =
            new NotificationQueryController(notificationQueryService, notificationEmitterService);

    @Test
    void inbox_엔드포인트는_USER_인증_어노테이션을_가진다() throws Exception {
        assertUserProtected("getNotifications", Pageable.class);
        assertUserProtected("getNotificationDetail", Long.class);
        assertUserProtected("getUnreadNotifications", Pageable.class);
        assertUserProtected("getUnreadCount");
    }

    @Test
    void 목록_pageable은_최신순_최대100으로_정규화된다() throws Exception {
        when(notificationQueryService.getNotifications(eq("user-1"),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                        .thenReturn(new PageImpl<>(List.of()));

        try (LoggedInUserContext ignored = LoggedInUserContext
                .open(new UserSecurityToken("user-1", "user-1@ticketon.site", Role.USER))) {
            controller.getNotifications(PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "sentAt")));
        }

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationQueryService).getNotifications(eq("user-1"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().toList()).hasSize(1);
        assertThat(pageable.getSort().toList().get(0).getProperty()).isEqualTo("sentAt");
        assertThat(pageable.getSort().toList().get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    private void assertUserProtected(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = NotificationQueryController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(AuthNeeded.class)).isNotNull();
        RoleRequired roleRequired = method.getAnnotation(RoleRequired.class);
        assertThat(roleRequired).isNotNull();
        assertThat(roleRequired.value()).containsExactly(Role.USER);
    }
}
