package org.codenbug.common.exception;

import lombok.Getter;

@Getter
public class FieldError {
  private final String fieldName;
  private final String rejectedValue;
  private final String message;

  public FieldError(String fieldName, String rejectedValue, String message) {
    this.fieldName = fieldName;
    this.rejectedValue = rejectedValue;
    this.message = message;
  }


}
