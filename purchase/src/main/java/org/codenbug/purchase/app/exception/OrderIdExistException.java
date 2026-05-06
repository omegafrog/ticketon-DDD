package org.codenbug.purchase.app.exception;

public class OrderIdExistException extends RuntimeException {

  public OrderIdExistException(String message) {
    super(message);
  }

  public OrderIdExistException(String message, Throwable cause) {
    super(message, cause);
  }

}
