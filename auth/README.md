# Auth Service

The Auth Service handles authentication, authorization, and user security management in the Ticketon system. It provides comprehensive authentication capabilities including traditional email/password login, OAuth integration with Google and Kakao, JWT token management, and user registration with event-driven user profile creation.

## 🎯 Purpose and Responsibilities

- **User Authentication**: Email/password and social OAuth login (Google, Kakao)
- **JWT Token Management**: Access token generation, refresh token handling, and token blacklisting
- **User Registration**: Account creation with validation and event publishing
- **Security Context**: User session management and security validation
- **OAuth Integration**: Social login provider abstraction and user info parsing
- **Event Publishing**: Domain events for user registration across services

## 🏗️ Architecture

### Domain Structure
```
auth/
├── domain/                       # Core security entities
│   ├── SecurityUser.java        # Main user entity with authentication info
│   ├── SecurityUserId.java      # Value object identifier
│   ├── SocialInfo.java          # Social login information
│   ├── Provider.java            # OAuth provider interface
│   ├── RefreshTokenBlackList.java # Token blacklist management
│   └── SecurityUserRepository.java
├── app/                          # Application services
│   ├── AuthService.java          # Core authentication logic
│   ├── OAuthService.java         # OAuth provider integration
│   ├── ProviderFactory.java     # Provider selection factory
│   └── *EventListener.java      # Event processing
├── ui/                          # REST controllers
│   ├── SecurityController.java  # Authentication endpoints
│   └── *Request.java           # Request/response DTOs
├── infra/                       # Infrastructure implementations
│   ├── GoogleProvider.java     # Google OAuth implementation
│   ├── KakaoProvider.java      # Kakao OAuth implementation
│   ├── RefreshTokenBlackListImpl.java
│   └── SecurityUserRepositoryImpl.java
├── consumer/                    # Event consumers
│   ├── UserRegisteredEventConsumer.java
│   └── UserRegisteredFailedEventConsumer.java
└── config/                      # Security configurations
    ├── SecurityConfig.java     # Spring Security setup
    ├── RedisConfig.java        # Redis token blacklist
    └── AuthConfig.java         # JWT settings
```

### Key Domain Concepts

**SecurityUser Aggregate**
- Root entity for authentication and authorization
- Contains both local (email/password) and social login information
- Manages account status, expiration, and login attempts
- Links to User domain through UserId

**Social Provider Strategy**
- Abstracted OAuth provider implementations
- Factory pattern for provider selection
- Standardized user info parsing across providers

**Token Management**
- JWT-based access tokens with user claims
- Refresh token rotation and blacklisting
- Secure cookie-based token storage

## 🔌 API Endpoints

### Authentication Operations
```
POST   /api/v1/auth/register                      # Email registration
POST   /api/v1/auth/login                         # Email/password login
GET    /api/v1/auth/logout                        # User logout
GET    /api/v1/auth/social/{socialLoginType}      # Social login request
GET    /api/v1/auth/social/{socialLoginType}/callback # OAuth callback
```

### Authentication Flow

**Email Registration & Login:**
```
1. POST /register → Creates SecurityUser → Publishes SecurityUserRegisteredEvent
2. POST /login → Validates credentials → Returns JWT tokens
3. Tokens stored: Access token in Authorization header, Refresh token in HttpOnly cookie
```

**Social Login Flow:**
```
1. GET /social/google → Returns OAuth authorization URL
2. User authorizes on provider → Redirected to callback
3. GET /social/google/callback → Exchanges code for tokens → Returns JWT
```

### Request/Response Examples

**Email Registration:**
```json
{
  "email": "user@example.com",
  "password": "secure_password",
  "name": "John Doe",
  "age": 25,
  "sex": "M",
  "phoneNum": "010-1234-5678",
  "location": "Seoul"
}
```

**Login Response:**
```json
{
  "resultCode": "200",
  "message": "login success",
  "data": "Bearer eyJhbGciOiJIUzI1NiJ9..."
}
```

**Social Login Response:**
```json
{
  "resultCode": "200-SUCCESS",
  "message": "소셜 로그인 성공",
  "data": "Bearer eyJhbGciOiJIUzI1NiJ9..."
}
```

## 🔧 Configuration

### Dependencies (build.gradle)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.kafka:spring-kafka'
    
    implementation project(':common')      # JWT utilities and shared code
    implementation project(':message')     # Domain events
    implementation project(':security-aop') # Security annotations
}
```

### Application Configuration
```yaml
# application.yml
spring:
  application:
    name: auth-service
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: ${GOOGLE_CALLBACK_URL}
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: ${KAKAO_CALLBACK_URL}

custom:
  jwt:
    secret: ${JWT_SECRET}
  cookie:
    domain: ${COOKIE_DOMAIN:localhost}

# Social login URLs
sns:
  google:
    url: https://accounts.google.com/oauth/v2/auth
    token.url: https://oauth2.googleapis.com/token
    client.id: ${GOOGLE_CLIENT_ID}
    client.secret: ${GOOGLE_CLIENT_SECRET}
    callback.url: ${GOOGLE_CALLBACK_URL}
  kakao:
    url: https://kauth.kakao.com/oauth/authorize
    token.url: https://kauth.kakao.com/oauth/token
    user.url: https://kapi.kakao.com/v2/user/me
    client.id: ${KAKAO_CLIENT_ID}
    client.secret: ${KAKAO_CLIENT_SECRET}
    callback.url: ${KAKAO_CALLBACK_URL}
```

## 🔗 Integration Points

### Internal Service Dependencies
- **Common Module**: JWT utilities, token generation, shared types
- **Message Module**: Domain event definitions and publishing
- **Security AOP**: Authentication annotations and context

### Domain Events Published
```java
// Email registration
SecurityUserRegisteredEvent {
    String securityUserId;
    String name;
    Integer age;
    String sex;
    String phoneNum;
    String location;
}

// Social registration
SnsUserRegisteredEvent {
    String securityUserId;
    String name;
    Integer age;
    String sex;
}
```

### Event Consumption
The service consumes user registration completion events:
```java
@KafkaListener(topics = "user-registered-success")
public void handleUserRegisteredSuccess(UserRegisteredEvent event) {
    // Update SecurityUser with User domain ID
}
```

## 💼 Business Rules and Validations

### Registration Rules
1. **Email Uniqueness**: Each email can only be used once
2. **Password Security**: Passwords are BCrypt encoded
3. **Social Account Linking**: Social accounts automatically create SecurityUser
4. **Event Publishing**: Registration triggers user profile creation

### Authentication Rules
1. **Account Status**: Must be enabled and not locked
2. **Password Matching**: BCrypt validation for password login
3. **Login Attempts**: Failed attempts tracking (future implementation)
4. **Token Expiration**: JWT tokens have configurable expiration

### Token Management Rules
1. **Access Token**: Short-lived, contains user claims
2. **Refresh Token**: Longer-lived, stored in HttpOnly cookie
3. **Token Blacklisting**: Logout adds tokens to Redis blacklist
4. **Secure Cookies**: Production uses secure, SameSite=None cookies

## 🎮 Usage Examples

### Email Authentication
```java
@PostMapping("/login")
public ResponseEntity<RsData<String>> login(@RequestBody LoginRequest request, HttpServletResponse resp) {
    TokenInfo tokenInfo = authService.loginEmail(request);
    
    // Set Authorization header
    resp.setHeader(HttpHeaders.AUTHORIZATION, 
        tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue());
    
    // Set refresh token cookie
    Cookie refreshToken = createRefreshTokenCookie(tokenInfo.getRefreshToken());
    resp.addCookie(refreshToken);
    
    return ResponseEntity.ok(new RsData<>("200", "login success", 
        tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue()));
}
```

### OAuth Provider Implementation
```java
@Component("GOOGLE")
public class GoogleProvider implements SocialProvider {
    
    @Override
    public String getOauthLoginUri() {
        Map<String, Object> params = new HashMap<>();
        params.put("scope", "profile email");
        params.put("response_type", "code");
        params.put("client_id", GOOGLE_CLIENT_ID);
        params.put("redirect_uri", GOOGLE_CALLBACK_URL);
        
        return GOOGLE_SNS_BASE_URL + "?" + buildQueryString(params);
    }
    
    @Override
    public UserInfo parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
        JsonNode jsonNode = objectMapper.readTree(userInfo);
        String socialId = jsonNode.get("sub").asText();
        String name = jsonNode.get("name").asText();
        String email = jsonNode.get("email").asText();
        
        return new UserInfo(socialId, name, socialLoginType.getName(), email, Role.USER.name(), 0, "ETC");
    }
}
```

## 🏃 Running the Service

The Auth service runs as a standalone Spring Boot application:

```bash
# Build the service
./gradlew :auth:build

# Run the service
./gradlew :auth:bootRun

# Build Docker image
./gradlew :auth:bootBuildImage

# Run with Docker
docker run -p 9001:9001 auth-service
```

### Service Port
- **Port 9001**: Auth service endpoint
- **Database**: MySQL for user storage
- **Cache**: Redis for token blacklisting

## 🔄 Event-Driven Architecture

### Event Flow
1. **Registration**: SecurityUser created → SecurityUserRegisteredEvent published
2. **User Service**: Consumes event → Creates User profile → UserRegisteredEvent published
3. **Auth Service**: Consumes completion event → Updates SecurityUser with User ID

### Event Processing
```java
@EventListener
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleSecurityUserRegistered(SecurityUserRegisteredEvent event) {
    // Process user registration after transaction commit
}
```

## 🔍 Monitoring and Observability

### Key Metrics to Monitor
- Login success/failure rates
- OAuth callback success rates
- Token generation and validation times
- User registration completion rates
- Social provider response times

### Security Logging
- Authentication attempts and failures
- Token blacklisting operations
- OAuth authorization flows
- Account lockout events
- Social provider API failures

## ⚠️ Security Considerations

### Password Security
- BCrypt hashing with salt rounds
- Password strength validation
- Secure password reset flow

### Token Security
- JWT signature validation
- Token expiration enforcement
- Refresh token rotation
- Blacklist cleanup processes

### OAuth Security
- State parameter validation (future implementation)
- Secure redirect URI validation
- Provider response validation
- Error handling without information leakage

### Cookie Security
- HttpOnly refresh tokens
- Secure flag in production
- SameSite=None for cross-origin requests
- Domain-specific cookie scoping

---

The Auth Service provides robust, secure authentication capabilities while maintaining clear separation of concerns and supporting both traditional and modern authentication patterns through its flexible OAuth provider system.