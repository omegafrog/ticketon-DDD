# Gateway Service

The Gateway Service acts as the single entry point for all client requests in the Ticketon system. Built with Spring Cloud Gateway, it provides request routing, JWT-based authentication, automatic token refresh, and centralized security enforcement across all microservices.

## 🎯 Purpose and Responsibilities

- **API Gateway**: Single entry point for all external requests (Port 8080)
- **Request Routing**: Route requests to appropriate microservices
- **JWT Authentication**: Validate access tokens and refresh tokens
- **Token Management**: Automatic token refresh and blacklist checking
- **CORS Handling**: Cross-origin resource sharing configuration
- **Security Enforcement**: Centralized authorization and whitelist management
- **Service Discovery**: Integration with Eureka for service location

## 🏗️ Architecture

### Service Structure
```
gateway/
├── config/                      # Configuration classes
│   ├── GatewayConfig.java      # CORS and gateway configuration
│   ├── WhitelistProperties.java # Whitelist URL configuration
│   └── RedisConfig.java        # Redis connection setup
├── filter/                     # Gateway filters
│   ├── AuthorizationFilter.java # Main authentication filter
│   ├── CustomErrorResponseFilter.java
│   └── RedisRefreshTokenBlackList.java # Token blacklist interface
├── infra/                      # Infrastructure implementations
│   └── RedisRefreshTokenBlackListImpl.java # Redis blacklist implementation
└── GatewayApplication.java     # Main application class
```

### Key Components

**AuthorizationFilter**
- JWT token validation and refresh
- Custom header injection (User-Id, Email, Role)
- Whitelist URL bypass
- Automatic token refresh on expiration
- Error handling and response formatting

**Gateway Routing**
- Route definitions for each microservice
- Load balancing for scalable services
- Path-based routing with predicates

**Token Blacklist**
- Redis-based refresh token blacklisting
- Logout token invalidation
- Security breach protection

## 🔌 Routing Configuration

### Service Routes
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:9001
          predicates:
            - Path=/api/v1/auth/**
        
        - id: broker-service  
          uri: lb://broker-service        # Load balanced via Eureka
          predicates:
            - Path=/api/v1/broker/**
            
        - id: main-service
          uri: http://localhost:9000      # App service (events, users, etc.)
          predicates:
            - Path=/api/v1/**
```

### Whitelist Configuration
```yaml
filter:
  whitelist:
    urls:
      - method: POST
        url: "/api/v1/auth/register"     # User registration
      - method: POST  
        url: "/api/v1/auth/login"        # User login
      - method: "*"
        url: "/api/v1/auth/social/**"    # Social login flows
      - method: POST
        url: "/api/v1/events"            # Public event listings
```

## 🔧 Configuration

### Dependencies (build.gradle)
```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    
    implementation project(':common')   # JWT utilities and shared code
}
```

### Application Configuration
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      default-filters: AuthorizationFilter  # Apply auth to all routes
      global-filter:
        response-timeout: 0                  # No timeout limit

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus     # Monitoring endpoints
```

## 🔒 Security Features

### JWT Token Processing

**Access Token Validation:**
```java
// 1. Extract access token from Authorization header
AccessToken accessToken = getAccessToken(request);

// 2. Validate token signature and expiration  
Util.validate(accessToken.getRawValue(), secretKey);

// 3. Decode claims and inject headers
accessToken.decode(jwtSecret);
ServerHttpRequest mutatedRequest = request.mutate()
    .header("User-Id", accessToken.getUserId())
    .header("Role", accessToken.getRole())
    .header("Email", accessToken.getEmail())
    .build();
```

**Automatic Token Refresh:**
```java
try {
    // Validate access token
    Util.validate(accessToken.getRawValue(), secretKey);
} catch (ExpiredJwtException e) {
    // Access token expired - refresh using refresh token
    RefreshToken refreshToken = getRefreshToken(request);
    TokenInfo newTokens = Util.refresh(refreshToken, secretKey, e.getCause());
    
    // Update tokens for downstream services
    accessToken = newTokens.getAccessToken();
    refreshToken = newTokens.getRefreshToken();
}
```

**Token Blacklist Checking:**
```java
// Check if refresh token is blacklisted (user logged out)
refreshTokenStorage.checkBlackList(refreshToken);

// If blacklisted, deny access
if (blacklisted) {
    throw new RuntimeException("Token is blacklisted");
}
```

### CORS Configuration
```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(Arrays.asList("*"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    
    return new CorsWebFilter(source);
}
```

## 🔗 Integration Points

### Internal Dependencies
- **Common Module**: JWT utilities, token parsing, validation logic
- **Redis**: Token blacklist storage and session management
- **Eureka Client**: Service discovery for load-balanced routes

### Header Injection
The gateway injects authentication context headers for downstream services:
```
User-Id: uuid-user-identifier
Role: USER|MANAGER|ADMIN  
Email: user@example.com
Authorization: Bearer jwt-access-token
```

### Service Communication
- **Eureka Integration**: Automatic service discovery and load balancing
- **WebFlux**: Non-blocking, reactive request processing
- **Circuit Breaker**: Future implementation for resilience

## 🎮 Authentication Flow

### Request Processing
```
1. Client Request → Gateway (Port 8080)
2. Check Whitelist → Bypass auth if whitelisted
3. Extract Tokens → Authorization header + refreshToken cookie
4. Validate Access Token → Check signature and expiration
5. Check Blacklist → Verify refresh token not blacklisted
6. Refresh if Needed → Generate new tokens if access token expired
7. Inject Headers → Add User-Id, Role, Email to request
8. Route Request → Forward to target microservice
9. Return Response → Include updated tokens in response
```

### Token Refresh Flow
```
1. Access Token Expired → ExpiredJwtException thrown
2. Extract Refresh Token → From HttpOnly cookie
3. Validate Refresh Token → Check signature and blacklist
4. Generate New Tokens → Create fresh access + refresh tokens
5. Update Response Headers → Set new Authorization header
6. Update Response Cookies → Set new refreshToken cookie
7. Continue Processing → Forward request with new tokens
```

## 🏃 Running the Service

The Gateway service runs as a standalone Spring Boot application:

```bash
# Build the service
./gradlew :gateway:build

# Run the service  
./gradlew :gateway:bootRun

# Build Docker image
./gradlew :gateway:bootBuildImage

# Run with Docker
docker run -p 8080:8080 gateway-service
```

### Service Dependencies
- **Port 8080**: Gateway service endpoint (main entry point)
- **Redis**: Required for token blacklist functionality
- **Eureka**: Optional for service discovery (can use direct URLs)
- **Target Services**: Auth (9001), App (9000), Broker (via Eureka)

## 🔍 Monitoring and Observability

### Health Checks
```
GET /actuator/health          # Gateway health status
GET /actuator/info           # Application information  
GET /actuator/prometheus     # Metrics for monitoring
```

### Key Metrics to Monitor
- Request throughput and latency
- Token validation success/failure rates
- Token refresh frequency
- Route-specific performance metrics
- Error rates by service

### Logging Points
- Authentication successes and failures
- Token refresh operations
- Blacklist checks and hits
- Route resolution and forwarding
- Error responses and causes

## ⚠️ Security Considerations

### Token Security
- **JWT Signature Validation**: All tokens validated with secret key
- **Token Blacklisting**: Refresh tokens invalidated on logout
- **Automatic Refresh**: Seamless token renewal for users
- **Secure Cookies**: HttpOnly, Secure flags in production

### CORS Security
- **Credential Support**: Allows cookies and authorization headers
- **Origin Validation**: Pattern-based origin checking
- **Method Restrictions**: Explicit HTTP method allowlist
- **Header Control**: Granular header access control

### Input Validation
- **Path Validation**: AntPathMatcher for secure pattern matching
- **Token Format**: Strict JWT format validation
- **Error Handling**: No information leakage in error responses

## 🔧 Configuration Management

### Environment-Specific Configuration
```yaml
# application-prod.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:9001    # Docker service names
        - id: main-service  
          uri: http://app-service:9000

custom:
  jwt:
    secret: ${JWT_SECRET}                  # Environment variable
```

### Whitelist Management
```yaml
filter:
  whitelist:
    urls:
      - method: POST
        url: "/api/v1/auth/**"
      - method: GET
        url: "/api/v1/public/**"
      - method: "*"
        url: "/health"
```

---

The Gateway Service provides secure, scalable request routing while maintaining a clean separation between public endpoints and authenticated resources. Its automatic token refresh capability ensures seamless user experience while maintaining strong security boundaries.