<!-- harness-reverse-engineered:v1 -->
# UC-001 Register
Actor: Visitor. Main: submit account/profile data; auth validates through user service, persists security account, publishes registration. Failure: duplicate or invalid data. Result: USER account/profile. Evidence: auth/ui/SecurityController.java; auth/app/AuthService.java; user/app/UserCommandService.java.
