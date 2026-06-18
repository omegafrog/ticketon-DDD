<!-- harness-reverse-engineered:v1 -->
# UC-002 Login
Actor: Visitor. Main: authenticate credentials or Google/Kakao callback; issue access and refresh tokens. Failure: bad credentials, suspended/deleted/locked account, unsupported provider. Evidence: auth/ui/SecurityController.java; auth/app/AuthService.java; auth/app/OAuthService.java.
