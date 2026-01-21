package org.codenbug.user.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codenbug.common.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import common.ValidationErrors;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Component("userGlobalExceptionHandler")
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ValidationErrors.class)
	public ResponseEntity<RsData<Map<String, Object>>> handleValidationErrors(ValidationErrors e) {
		List<String> messages = e.getErrors().stream()
				.map(Throwable::getMessage)
				.collect(Collectors.toList());

		return ResponseEntity.badRequest().body(new RsData<>(
				HttpStatus.BAD_REQUEST.toString(),
				"파라미터 validation 실패했습니다.",
				Map.of("errors", messages)));
	}
}
