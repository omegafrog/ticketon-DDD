# Common Module

The Common Module provides shared utilities, services, and infrastructure components used across all microservices in the Ticketon system. It contains JWT token management, Redis-based distributed locking, standardized response formats, exception handling, and core utilities that enable consistent behavior across the distributed architecture.

## 🎯 Purpose and Responsibilities

- **JWT Token Management**: Token creation, validation, parsing, and refresh functionality
- **Distributed Locking**: Redis-based seat reservation and concurrency control
- **Queue Management**: Entry token validation for waiting queue system
- **Response Standardization**: Uniform API response format across services
- **Exception Handling**: Centralized exception types and error handling
- **Shared Utilities**: UUID generation, common enums, and utility functions

## 🏗️ Architecture

### Module Structure
```
common/
├── jwt & auth/                 # JWT token management
│   ├── AccessToken.java       # Access token value object
│   ├── RefreshToken.java      # Refresh token value object
│   ├── TokenInfo.java         # Token pair container
│   └── Util.java             # JWT utilities and operations
├── redis/                     # Redis services
│   ├── RedisLockService.java         # Distributed locking interface
│   ├── RedisLockServiceImpl.java     # Lock implementation
│   ├── EntryTokenValidator.java      # Queue token validation
│   ├── RedisKeyScanner.java         # Key scanning utilities
│   └── RedisConfig.java             # Redis configuration
├── shared types/              # Common data types
│   ├── RsData.java           # Standardized API response
│   └── Role.java             # User role enumeration
└── exception/                 # Exception hierarchy
    ├── BaseException.java    # Base exception class
    ├── JwtException.java     # JWT-related exceptions
    ├── ExpiredJwtException.java
    ├── SignatureException.java
    └── AccessDeniedException.java
```

### Key Components

**JWT Token System**
- HMAC-SHA256 signed tokens with configurable expiration
- Automatic token refresh capabilities
- Claims-based user information storage
- UUIDv7-based token identifiers

**Redis Distributed Locking**
- Atomic seat reservation operations
- TTL-based automatic lock release
- User-specific lock management
- Queue entry token validation

**Standardized Response Format**
- Consistent API response structure
- Error code and message standardization
- Generic data payload support

## 🔌 Core Utilities

### JWT Token Management

**Token Generation:**
```java
public static TokenInfo generateTokens(Map<String, Object> claims, SecretKey secretKey) {
    AccessToken accessToken = getAccessToken(claims, secretKey);
    RefreshToken refreshToken = getRefreshToken(
        Map.of("access-jti", accessToken.getClaims().getId()), secretKey);
    return new TokenInfo(accessToken, refreshToken);
}
```

**Token Validation:**
```java
public static void validate(String rawValue, SecretKey secretKey) {
    try {
        Jwts.parser().verifyWith(secretKey).build().parse(rawValue);
    } catch (ExpiredJwtException e) {
        throw new org.codenbug.common.exception.ExpiredJwtException("401", "Jwt is expired", e);
    } catch (SignatureException e) {
        throw new org.codenbug.common.exception.SignatureException();
    }
}
```

**Token Refresh:**
```java
public static TokenInfo refresh(RefreshToken refreshToken, SecretKey secretKey, Throwable e) {
    if (e instanceof ExpiredJwtException) {
        ExpiredJwtException ex = (ExpiredJwtException) e;
        Claims claims = ex.getClaims();
        
        // Extract user information from expired token
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);
        String email = claims.get("email", String.class);
        
        // Generate new tokens
        return generateTokens(Map.of("userId", userId, "role", role, "email", email), secretKey);
    }
}
```

### Distributed Locking

**Seat Reservation Lock:**
```java
public boolean tryLock(String key, String value, Duration timeout) {
    return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
}

public boolean unlock(String key, String value) {
    String storedValue = redisTemplate.opsForValue().get(key);
    if (value != null && value.equals(storedValue)) {
        redisTemplate.delete(key);
        return true;
    }
    return false;
}
```

**Lock Key Format:**
```
seat:lock:{userId}:{eventId}:{seatId}
```

**User Lock Management:**
```java
public void releaseAllLocks(String userId) {
    Set<String> keys = redisKeyScanner.scanKeys("seat:lock:" + userId + ":*");
    if (keys != null) {
        redisTemplate.delete(keys);
    }
}
```

### Entry Token Validation

**Queue Token Verification:**
```java
public void validate(String userId, String token) {
    String storedToken = redisTemplate.opsForHash()
        .get("ENTRY_TOKEN", userId).toString();
    
    if (storedToken == null || !storedToken.replace("\"", "").equals(token)) {
        throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
    }
}
```

## 🔧 Configuration

### Dependencies (build.gradle)
```gradle
dependencies {
    // JWT token processing
    api("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    
    // Redis integration
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Lombok for boilerplate reduction
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### Redis Configuration
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 🎮 Usage Examples

### Token Operations
```java
// Generate tokens for authenticated user
Map<String, Object> claims = Map.of(
    "userId", "user-uuid",
    "role", Role.USER,
    "email", "user@example.com"
);
SecretKey secretKey = Util.Key.convertSecretKey(jwtSecret);
TokenInfo tokenInfo = Util.generateTokens(claims, secretKey);

// Parse tokens from HTTP requests
AccessToken accessToken = Util.parseAccessToken("Bearer jwt-token");
RefreshToken refreshToken = Util.parseRefreshToken("refresh-token-value");

// Validate token
try {
    Util.validate(accessToken.getRawValue(), secretKey);
} catch (ExpiredJwtException e) {
    TokenInfo refreshed = Util.refresh(refreshToken, secretKey, e.getCause());
}
```

### Distributed Locking
```java
@Component
public class SeatReservationService {
    private final RedisLockService lockService;
    
    public void reserveSeat(String userId, String eventId, String seatId) {
        String lockKey = "seat:lock:" + userId + ":" + eventId + ":" + seatId;
        String lockValue = UUID.randomUUID().toString();
        
        if (lockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5))) {
            try {
                // Perform seat reservation
            } finally {
                lockService.unlock(lockKey, lockValue);
            }
        } else {
            throw new IllegalStateException("Seat is already reserved");
        }
    }
}
```

### Standardized Responses
```java
@RestController
public class ExampleController {
    
    @GetMapping("/data")
    public ResponseEntity<RsData<String>> getData() {
        return ResponseEntity.ok(new RsData<>("200", "Success", "data payload"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleError(Exception e) {
        return ResponseEntity.badRequest()
            .body(new RsData<>("400", e.getMessage(), null));
    }
}
```

## 🔗 Integration Points

### Service Dependencies
All microservices in the Ticketon system depend on the common module:

**Direct Dependencies:**
- Auth Service: JWT token generation and validation
- Gateway Service: Token parsing and validation in filters
- Seat Service: Distributed locking for seat reservations
- Broker Service: Entry token validation for queue management

**Transitive Usage:**
- Event-driven services use RsData for consistent responses
- All services use common exception types
- UUID generation used throughout domain entities

## 💼 Business Rules and Constants

### Token Configuration
```java
public static final long REFRESH_TOKEN_EXP = 60 * 60 * 24 * 7; // 7 days
private static final int ACCESS_TOKEN_EXP = 60 * 30; // 30 minutes
```

### Lock Configuration
```java
private static final String PREFIX = "seat:lock:";
public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";
```

### Role Enumeration
```java
public enum Role {
    USER,    // Regular users
    MANAGER, // Event managers
    ADMIN    // System administrators
}
```

## ⚠️ Security Considerations

### JWT Security
- **HMAC-SHA256**: Cryptographically secure signing algorithm
- **Short-lived Access Tokens**: 30-minute expiration reduces exposure
- **Rotating Refresh Tokens**: New refresh token on each refresh
- **JTI Claims**: Unique token identifiers prevent replay attacks

### Lock Security
- **UUID Lock Values**: Prevents unauthorized lock release
- **TTL Expiration**: Automatic cleanup prevents deadlocks
- **Atomic Operations**: Redis operations ensure consistency
- **User Isolation**: Locks are user-specific

### Input Validation
- **Token Format Validation**: Strict JWT format checking
- **Key Validation**: Secure key conversion and storage
- **Exception Sanitization**: No sensitive information in error messages

## 🔍 Monitoring and Observability

### Key Metrics to Monitor
- JWT token generation/validation rates
- Token refresh frequency
- Lock acquisition success/failure rates
- Redis connection health
- Exception rates by type

### Performance Considerations
- **Redis Connection Pooling**: Efficient connection management
- **Token Caching**: Claims caching for frequently accessed tokens
- **Batch Lock Operations**: Efficient multi-lock management
- **Key Scanning Optimization**: Pattern-based key searches

## 🏃 Library Usage

The common module is designed as a shared library:

```bash
# Build the library
./gradlew :common:build

# The module is included in other services via:
# implementation project(':common')

# It doesn't run standalone (bootJar disabled)
```

### Version Management
- Semantic versioning for compatibility
- Backward compatibility maintenance
- Breaking changes documented
- Migration guides provided

---

The Common Module serves as the foundation for the entire Ticketon system, providing essential utilities and ensuring consistency across all microservices through shared abstractions and standardized implementations.