package org.codenbug.broker.controller;

import org.codenbug.common.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<RsData<Void>> handleRuntimeException(RuntimeException ex) {
		return new ResponseEntity<>(new RsData<>("400", ex.getMessage(), null), HttpStatus.BAD_REQUEST);
	}
}
