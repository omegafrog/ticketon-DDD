package org.codenbug.notification.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codenbug.notification.domain.entity.NotificationType;

/**
 * 메서드 실행 후 사용자에게 알림을 전송하는 AOP 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotifyUser {

    /**
     * 알림 유형
     */
    NotificationType type();

    /**
     * 알림 제목
     */
    String title();

    /**
     * 알림 내용
     */
    String content();

    /**
     * 알림 클릭 시 이동할 URL (선택사항)
     */
    String targetUrl() default "";

    /**
     * 사용자 ID를 추출할 수 없을 때 사용할 기본 사용자 ID
     */
    String defaultUserId() default "";

    /**
     * SpEL 표현식으로 사용자 ID 추출 예: "#result.userId", "#userId"
     */
    String userIdExpression() default "#result.userId";
}
