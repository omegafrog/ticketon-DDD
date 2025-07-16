package org.codenbug.auth.ui;

import org.codenbug.common.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({EntityNotFoundException.class})
	public ResponseEntity<RsData<Void>> entityNotFoundException(EntityNotFoundException ex){
		return ResponseEntity.badRequest().body(new RsData<>("400", ex.getMessage(), null));
	}
}
