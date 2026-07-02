package org.codenbug.notification.infra.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.codenbug.notification.application.NotificationCommandService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotificationAspect {

    private final NotificationCommandService notificationCommandService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Pointcut("@annotation(notifyUser)")
    public void notificationPointcut(NotifyUser notifyUser) {}

    @Pointcut("@annotation(notifyUser)")
    public void legacyNotificationPointcut(org.codenbug.notification.aop.NotifyUser notifyUser) {}

    @AfterReturning(pointcut = "notificationPointcut(notifyUser)", returning = "result")
    public void sendNotification(NotifyUser notifyUser, Object result) {
        sendNotification(notifyUser.type(), notifyUser.title(), notifyUser.content(),
                notifyUser.targetUrl(), notifyUser.userIdExpression(), notifyUser.defaultUserId(),
                result);
    }

    @AfterReturning(pointcut = "legacyNotificationPointcut(notifyUser)", returning = "result")
    public void sendLegacyNotification(org.codenbug.notification.aop.NotifyUser notifyUser,
            Object result) {
        sendNotification(notifyUser.type(), notifyUser.title(), notifyUser.content(),
                notifyUser.targetUrl(), notifyUser.userIdExpression(), notifyUser.defaultUserId(),
                result);
    }

    private void sendNotification(org.codenbug.notification.domain.entity.NotificationType type,
            String title, String content, String targetUrl, String userIdExpression,
            String defaultUserId, Object result) {
        try {
            String userId = extractUserId(result, userIdExpression, defaultUserId);
            if (userId != null && !userId.trim().isEmpty()) {
                notificationCommandService.createNotificationWithoutResult(userId, type, title, content,
                        targetUrl.isEmpty() ? null : targetUrl);
                log.debug("알림 생성 완료: userId={}, type={}", userId, type);
            } else {
                log.debug("유효하지 않은 userId로 인해 알림 생성 건너뜀: {}", userId);
            }
        } catch (Exception e) {
            log.error("알림 생성 중 오류 발생", e);
        }
    }

    private String extractUserId(Object result, String userIdExpression, String defaultUserId) {
        try {
            Expression expression = parser.parseExpression(userIdExpression);
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("result", result);

            Object userId = expression.getValue(context);
            if (userId != null) {
                return userId.toString();
            }
        } catch (Exception e) {
            log.debug("SpEL 표현식으로 userId 추출 실패: {}, 기본값 사용", userIdExpression);
        }

        return (defaultUserId != null && !defaultUserId.trim().isEmpty()) ? defaultUserId : null;
    }
}
