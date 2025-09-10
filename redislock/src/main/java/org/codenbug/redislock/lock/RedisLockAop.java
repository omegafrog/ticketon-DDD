package org.codenbug.redislock.lock;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import io.lettuce.core.dynamic.support.ParameterNameDiscoverer;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class RedisLockAop {

	public final RedissonClient redissonClient;
	private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
	private final ExpressionParser parser = new SpelExpressionParser();
	public RedisLockAop(RedissonClient redissonClient) {
		this.redissonClient = redissonClient;
	}

	@Around("@annotation(redisLock)")
	public Object redisLockAop(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
		String keyExpression = redisLock.key();
		StringBuilder lockKeyBuilder = new StringBuilder();
		if(keyExpression.startsWith("#")){
			lockKeyBuilder.append(extractKeyFromMethodParam(joinPoint,  keyExpression));
		}else{
			lockKeyBuilder.append(keyExpression);
		}
		String lockKey = generateLockKey(lockKeyBuilder.toString());
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(redisLock.waitTime(), redisLock.leaseTime(), TimeUnit.SECONDS);
			if (!acquired) {
				throw new IllegalStateException("락 획득 실패.");
			}

			log.info("락 획득 성공: {}", lockKey);
			return joinPoint.proceed();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("락 대기 중 인터럽트 발생", e);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.info("락 해제: {}", lockKey);
			}
		}
	}

	private String extractKeyFromMethodParam(ProceedingJoinPoint joinPoint, String keyExpression) {
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

		// 파라미터 이름과 값 매핑
		EvaluationContext context = new StandardEvaluationContext();
		String[] paramNames = nameDiscoverer.getParameterNames(method);
		Object[] args = joinPoint.getArgs();
		if (paramNames != null) {
			for (int i = 0; i < paramNames.length; i++) {
				context.setVariable(paramNames[i], args[i]);
			}
		}
		return parser.parseExpression(keyExpression).getValue(context, String.class);
	}

	private String generateLockKey(String key) {
		return String.format("lock:%s", key);
	}
}
