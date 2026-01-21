package org.codenbug.auth.global;

import java.util.HashMap;
import java.util.Map;

import org.codenbug.common.RsData;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<RsData<Map<String, Object>>> handleValidationExceptions(Exception ex) {
        Map<String, Object> errors = new HashMap<>();

        if (ex instanceof MethodArgumentNotValidException) {
            ((MethodArgumentNotValidException) ex).getBindingResult().getFieldErrors()
                    .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        } else {
            ((BindException) ex).getBindingResult().getFieldErrors()
                    .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        }

        return ResponseEntity.badRequest().body(new RsData<>(
                HttpStatus.BAD_REQUEST.toString(),
                "파라미터 validation 실패했습니다.",
                errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("REQUEST BODY NOT READABLE for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage());

        return ResponseEntity.badRequest()
                .body(new RsData<>(HttpStatus.BAD_REQUEST.toString(),
                        "요청 바디를 확인해주세요.",
                        null));
    }

    @ExceptionHandler({EntityNotFoundException.class, AccessDeniedException.class})
    public ResponseEntity<RsData<Void>> handleLoginFailure(Exception e, HttpServletRequest request) {
        log.error("LOGIN FAILED for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new RsData<>(HttpStatus.UNAUTHORIZED.toString(),
                        "인증에 실패했습니다.",
                        null));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<RsData<Void>> handleAsyncRequestTimeout(AsyncRequestTimeoutException e,
            HttpServletRequest request) {
        log.error("REQUEST TIMEOUT occurred for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(new RsData<>(HttpStatus.REQUEST_TIMEOUT.toString(),
                        "Request Timeout",
                        null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RsData<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("DATA INTEGRITY VIOLATION for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST.toString(),
                        "요청이 유효하지 않습니다.",
                        null));
    }

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<RsData<?>> handleUserValidationException(UserValidationException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getRsData());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error(
                "GENERAL EXCEPTION occurred for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RsData<>(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        "Internal Server Error",
                        null));
    }
}
