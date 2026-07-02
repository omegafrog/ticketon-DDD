package org.codenbug.notification.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codenbug.notification.domain.entity.NotificationType;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotifyUser {

    NotificationType type();

    String title();

    String content();

    String targetUrl() default "";

    String defaultUserId() default "";

    String userIdExpression() default "#result.userId";
}
