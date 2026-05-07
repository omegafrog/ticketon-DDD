package org.codenbug.purchase.ui.advice;

import org.codenbug.purchase.app.exception.OrderExistException;
import org.codenbug.purchase.app.exception.OrderIdExistException;
import org.codenbug.common.RsData;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PurchaseExceptionAdvice {

  @ExceptionHandler(OrderExistException.class)
  public ResponseEntity<RsData<Void>> UnprocesssedOrderExistException(OrderExistException ex) {

    return ResponseEntity.badRequest().body(new RsData<>("400", ex.getMessage(), null));

  }

  @ExceptionHandler(OrderIdExistException.class)
  public ResponseEntity<RsData<Void>> orderIdExistException(OrderIdExistException ex) {
    return ResponseEntity.badRequest().body(new RsData<>("400", ex.getMessage(), null));
  }
}
