package org.codenbug.notification.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.codenbug.notification.service.NotificationService;
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

    private final NotificationService notificationService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Pointcut("@annotation(notifyUser)")
    public void notificationPointcut(NotifyUser notifyUser) {}

    @AfterReturning(pointcut = "notificationPointcut(notifyUser)", returning = "result")
    public void sendNotification(NotifyUser notifyUser, Object result) {
        try {
            String userId = extractUserId(result, notifyUser);
            if (userId != null && !userId.trim().isEmpty()) {
                notificationService.createNotification(userId, notifyUser.type(),
                        notifyUser.title(), notifyUser.content(),
                        notifyUser.targetUrl().isEmpty() ? null : notifyUser.targetUrl());
                log.debug("알림 생성 완료: userId={}, type={}", userId, notifyUser.type());
            } else {
                log.debug("유효하지 않은 userId로 인해 알림 생성 건너뜀: {}", userId);
            }
        } catch (Exception e) {
            log.error("알림 생성 중 오류 발생", e);
        }
    }

    private String extractUserId(Object result, NotifyUser notifyUser) {
        try {
            // SpEL 표현식으로 사용자 ID 추출
            Expression expression = parser.parseExpression(notifyUser.userIdExpression());
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("result", result);

            Object userId = expression.getValue(context);
            if (userId != null) {
                return userId.toString();
            }
        } catch (Exception e) {
            log.debug("SpEL 표현식으로 userId 추출 실패: {}, 기본값 사용", notifyUser.userIdExpression());
        }

        // 기본값 사용
        String defaultUserId = notifyUser.defaultUserId();
        return (defaultUserId != null && !defaultUserId.trim().isEmpty()) ? defaultUserId : null;
    }
}
