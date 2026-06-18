<!-- harness-reverse-engineered:v1 -->
# UC-023 Backoffice Login
Actor: Administrator. Main: submit credentials and receive HTTP session. Failure: wrong role/credentials or locked account. Three failures lock account. Evidence: auth/ui/AdminBackofficeController.java; auth/domain/SecurityUser.java.
