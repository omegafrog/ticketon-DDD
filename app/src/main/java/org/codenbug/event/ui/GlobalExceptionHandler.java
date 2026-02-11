package org.codenbug.event.ui;

import java.util.HashMap;
import java.util.Map;

import org.codenbug.common.RsData;
import org.codenbug.common.exception.ControllerParameterValidationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({MethodArgumentNotValidException.class,
      ControllerParameterValidationFailedException.class, BindException.class})
  public ResponseEntity<RsData<Map<String, Object>>> handleValidationExceptions(Exception ex) {
    if (ex instanceof MethodArgumentNotValidException) {
      return handleMethodArgumentNotValidException((MethodArgumentNotValidException) ex);
    } else if (ex instanceof ControllerParameterValidationFailedException) {
      return handleControllerParameterValidationFailedException(
          (ControllerParameterValidationFailedException) ex);
    } else {
      return handleBindException((BindException) ex);
    }
  }

  private ResponseEntity<RsData<Map<String, Object>>> handleBindException(BindException ex) {
    Map<String, Object> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error -> {
      errors.put(error.getField(), error.getDefaultMessage());
    });
    return ResponseEntity.badRequest().body(new RsData<Map<String, Object>>(
        HttpStatus.BAD_REQUEST.toString(), "파라미터 validation 실패했습니다.", errors));
  }

  private ResponseEntity<RsData<Map<String, Object>>> handleControllerParameterValidationFailedException(
      ControllerParameterValidationFailedException ex) {
    Map<String, Object> errors = new HashMap<>();

    ex.getFieldErrors().forEach(error -> errors.put(error.getFieldName(), error.getMessage()));
    return ResponseEntity.badRequest().body(new RsData<Map<String, Object>>(
        HttpStatus.BAD_REQUEST.toString(), "파라미터 validation 실패했습니다.", errors));
  }

  private ResponseEntity<RsData<Map<String, Object>>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    Map<String, Object> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error -> {
      errors.put(error.getField(), error.getDefaultMessage());
    });
    return ResponseEntity.badRequest().body(new RsData<Map<String, Object>>(
        ex.getStatusCode().toString(), "파라미터 validation 실패했습니다.", errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RsData<Map<String, Object>>> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    Map<String, Object> errors = new HashMap<>();
    errors.put("message", "요청 본문 형식이 올바르지 않습니다.");
    return ResponseEntity.badRequest().body(new RsData<Map<String, Object>>(
        HttpStatus.BAD_REQUEST.toString(), "요청 본문 형식이 올바르지 않습니다.", errors));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<RsData<Map<String, Object>>> handleIllegalStateException(IllegalStateException ex) {
    Map<String, Object> errors = new HashMap<>();
    errors.put("message", ex.getMessage());
    return ResponseEntity.badRequest().body(new RsData<Map<String, Object>>(
        HttpStatus.BAD_REQUEST.toString(), "파라미터 validation 실패했습니다.", errors));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<RsData<Map<String, Object>>> handleEntityNotFoundException(
      EntityNotFoundException ex) {
    Map<String, Object> errors = new HashMap<>();
    errors.put("message", ex.getMessage() == null ? "리소스를 찾을 수 없습니다." : ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RsData<Map<String, Object>>(
        HttpStatus.NOT_FOUND.toString(), "리소스를 찾을 수 없습니다.", errors));
  }

  @ExceptionHandler(JpaObjectRetrievalFailureException.class)
  public ResponseEntity<RsData<Map<String, Object>>> handleJpaObjectRetrievalFailureException(
      JpaObjectRetrievalFailureException ex) {
    Map<String, Object> errors = new HashMap<>();
    errors.put("message", "리소스를 찾을 수 없습니다.");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RsData<Map<String, Object>>(
        HttpStatus.NOT_FOUND.toString(), "리소스를 찾을 수 없습니다.", errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<RsData<Map<String, Object>>> handleUnhandledException(Exception ex) {
    Map<String, Object> errors = new HashMap<>();
    errors.put("message", "서버 내부 오류가 발생했습니다.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new RsData<Map<String, Object>>(
        HttpStatus.INTERNAL_SERVER_ERROR.toString(), "서버 내부 오류가 발생했습니다.", errors));
  }



}
