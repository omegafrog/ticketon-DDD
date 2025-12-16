package org.codenbug.common.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class ControllerParameterValidationFailedException extends BaseException {
  private final List<? extends FieldError> fieldErrors;

  public ControllerParameterValidationFailedException(String message,
      List<? extends FieldError> fieldErrors) {
    super("400", message);
    this.fieldErrors = fieldErrors;
  }

}
