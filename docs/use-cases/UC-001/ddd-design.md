<!-- harness-reverse-engineered:v1 -->
# DDD Design
Entities: SecurityUser, User. VOs: SecurityUserId, UserId, SocialInfo. Behavior: register and validate identity/profile. Flow: SecurityController -> AuthService -> repository/message -> UserCommandService. Aggregates: SecurityUser, User. Contexts: Auth, User.
