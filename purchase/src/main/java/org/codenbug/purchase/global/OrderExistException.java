package org.codenbug.purchase.global;

public class OrderExistException extends RuntimeException {

  public OrderExistException(String message, Throwable cause) {
    super(message, cause);
  }

  public OrderExistException(String message) {
    super(message);
  }

}
