package org.codenbug.auth.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<String> handleAsyncRequestTimeout(AsyncRequestTimeoutException e,
            HttpServletRequest request) {
        log.error("REQUEST TIMEOUT occurred for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body("{\"timestamp\":\"" + java.time.Instant.now()
                        + "\",\"status\":408,\"error\":\"Request Timeout\",\"path\":\""
                        + request.getRequestURI() + "\"}");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error(
                "GENERAL EXCEPTION occurred for URI: {} - Method: {} - RemoteAddr: {} - Error: {}",
                request.getRequestURI(), request.getMethod(), request.getRemoteAddr(),
                e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"timestamp\":\"" + java.time.Instant.now()
                        + "\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\""
                        + request.getRequestURI() + "\"}");
    }
}
