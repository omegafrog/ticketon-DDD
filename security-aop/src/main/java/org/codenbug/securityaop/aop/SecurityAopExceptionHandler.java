package org.codenbug.securityaop.aop;

import org.codenbug.common.RsData;
import org.codenbug.common.exception.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Component("securityAopExceptionHandler")
@Slf4j
public class SecurityAopExceptionHandler {

	@ExceptionHandler({IllegalArgumentException.class,
			org.codenbug.common.exception.JwtException.class, JwtException.class})
	public ResponseEntity<RsData<Void>> handleInvalidToken(Exception e) {
		log.error("INVALID TOKEN: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new RsData<>(HttpStatus.UNAUTHORIZED.toString(),
						"인증 토큰이 유효하지 않습니다.",
						null));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<RsData<Void>> handleAccessDenied(AccessDeniedException e) {
		log.error("ACCESS DENIED: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new RsData<>(HttpStatus.FORBIDDEN.toString(),
						"접근 권한이 없습니다.",
						null));
	}
}
