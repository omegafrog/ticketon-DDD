package org.codenbug.auth.app;

import org.codenbug.auth.ui.RegisterRequest;

public interface UserRegistrationValidator {

    void validateRegisterInputs(RegisterRequest request);
}
