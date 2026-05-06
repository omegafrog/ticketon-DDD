package org.codenbug.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class NotificationDomainServiceTest {

    @Test
    void 도메인_서비스는_스프링_스테레오타입을_가지지_않는다() {
        assertThat(Arrays.stream(NotificationDomainService.class.getAnnotations())
                .map(annotation -> annotation.annotationType().getName())
                .filter(annotationName -> annotationName.startsWith("org.springframework.stereotype."))
                .toList()).isEmpty();
    }
}
