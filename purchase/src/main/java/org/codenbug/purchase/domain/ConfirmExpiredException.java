package org.codenbug.purchase.domain;

public class ConfirmExpiredException extends RuntimeException {

  public ConfirmExpiredException(String message) {
    super(message);
  }

  public ConfirmExpiredException(String message, Throwable cause) {
    super(message, cause);
  }

}
