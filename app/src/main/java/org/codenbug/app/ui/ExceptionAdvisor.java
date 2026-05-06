package org.codenbug.app.ui;

import java.util.HashMap;
import java.util.Map;

import org.codenbug.common.RsData;
import org.codenbug.common.exception.BaseException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice(basePackages = { "org.codenbug.user", "org.codenbug.event", "org.codenbug.purchase",
    "org.codenbug.notification", "org.codenbug.app" })
public class ExceptionAdvisor {

  @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
  public ResponseEntity<RsData<Map<String, Object>>> handleValidation(Exception ex) {
    Map<String, Object> errors = new HashMap<>();

    if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
      methodArgumentNotValidException.getBindingResult().getFieldErrors()
          .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    } else if (ex instanceof BindException bindException) {
      bindException.getBindingResult().getFieldErrors()
          .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new RsData<>(String.valueOf(HttpStatus.BAD_REQUEST.value()), "요청 값 검증에 실패했습니다.", errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<RsData<Map<String, Object>>> handleConstraintViolation(
      ConstraintViolationException ex) {
    Map<String, Object> errors = new HashMap<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      errors.put(violation.getPropertyPath().toString(), violation.getMessage());
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new RsData<>(String.valueOf(HttpStatus.BAD_REQUEST.value()), "요청 값 검증에 실패했습니다.", errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RsData<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new RsData<>(String.valueOf(HttpStatus.BAD_REQUEST.value()), "요청 본문 형식이 올바르지 않습니다.", null));
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<RsData<Void>> handleBaseException(BaseException ex) {
    HttpStatus status = resolveStatus(ex.getCode());
    return ResponseEntity.status(status)
        .body(new RsData<>(String.valueOf(status.value()), ex.getMessage(), null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<RsData<Void>> handleUnhandled(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new RsData<>(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "서버 내부 오류가 발생했습니다.", null));
  }

  private HttpStatus resolveStatus(String code) {
    if (code == null) {
      return HttpStatus.BAD_REQUEST;
    }

    try {
      return HttpStatus.valueOf(Integer.parseInt(code));
    } catch (NumberFormatException ex) {
      return HttpStatus.BAD_REQUEST;
    }
  }
}
