package org.codenbug.purchase.domain;

public class ConfirmExpiredException extends IllegalStateException {

  public ConfirmExpiredException(String message) {
    super(message);
  }

  public ConfirmExpiredException(String message, Throwable cause) {
    super(message, cause);
  }

}
