<!-- harness-reverse-engineered:v1 -->
# UC-003 Session Tokens
Actor: Authenticated user. Main: refresh token pair or log out. Failure: invalid/blacklisted token. Result: renewed tokens or invalidated refresh token. Evidence: auth/ui/SecurityController.java; auth/domain/RefreshTokenBlackList.java.
