# User Service

The User Service manages user profile information and acts as a bridge between the Auth service and the application domain. It handles user profile creation, retrieval, and maintains the relationship between security users and business user profiles through an event-driven architecture.

## 🎯 Purpose and Responsibilities

- **User Profile Management**: Store and manage user personal information
- **Auth Integration**: Bridge between SecurityUser (auth) and User (business) domains
- **Event-Driven Creation**: React to user registration events from Auth service
- **Profile Queries**: Provide user information for authenticated users
- **Data Consistency**: Maintain referential integrity between auth and user domains

## 🏗️ Architecture

### Domain Structure
```
user/
├── domain/                     # Core business entities
│   ├── User.java              # Main user profile entity
│   ├── UserId.java            # Value object identifier (UUIDv7)
│   ├── SecurityUserId.java    # Link to auth domain
│   ├── Sex.java               # Gender enumeration
│   └── UserRepository.java    # Repository interface
├── app/                       # Application services
│   ├── UserRegisterService.java        # User registration logic
│   ├── UserCommandQueryService.java    # Query operations
│   └── UserRegistrationEventListener.java # Event handling
├── consumer/                   # Event consumers
│   ├── SecurityUserRegisteredConsumer.java # Email registration events
│   └── SnsUserRegisteredConsumer.java     # Social login events
├── ui/                        # REST controllers
│   ├── UserController.java    # User profile endpoints
│   └── RegisterRequest.java   # DTOs
├── infra/                     # Infrastructure layer
│   ├── UserRepositoryImpl.java
│   └── JpaUserRepository.java
└── global/dto/               # Shared DTOs
    └── UserInfo.java         # User information response
```

### Key Domain Concepts

**User Aggregate**
- Contains personal profile information (name, age, sex, phone, location)
- Links to SecurityUser through SecurityUserId
- Maintains audit trails with creation/modification timestamps
- Uses UUIDv7 for identifiers

**Event-Driven Creation**
- Users are created only through event consumption
- Supports both email and social registration flows
- Implements compensation transactions for failures

**Domain Separation**
- User domain focuses on profile/business data
- Auth domain handles authentication/security concerns
- Clear separation of concerns with event-based communication

## 🔌 API Endpoints

### User Operations
```
GET    /api/v1/users/me          # Get current user profile
```

### Authentication Requirements
- All endpoints require `@AuthNeeded` authentication
- Uses `@RoleRequired(Role.USER)` for access control
- User context provided through `LoggedInUserContext`

### Response Examples

**User Profile Response:**
```json
{
  "resultCode": "200",
  "message": "User info",
  "data": {
    "userId": "uuid-user-id",
    "name": "John Doe",
    "sex": "MALE",
    "phoneNum": "010-1234-5678",
    "location": "Seoul",
    "age": 25,
    "securityUserId": "uuid-security-user-id",
    "createdAt": "2024-01-20T10:30:00",
    "modifiedAt": "2024-01-20T10:30:00"
  }
}
```

## 🔧 Configuration

### Dependencies (build.gradle)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    
    implementation project(':common')      # Shared utilities
    implementation project(':message')     # Domain events
    implementation project(':security-aop') # Authentication/authorization
}
```

### Application Configuration
```yaml
# user.yml
spring:
  application:
    name: user-service
  jpa:
    hibernate:
      ddl-auto: validate
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:root}
    password: ${RABBITMQ_PASSWORD:root}
```

## 🔗 Integration Points

### Event-Driven Integration

**Inbound Events (Consumed):**
```java
@RabbitListener(queues = "security-user-registered")
SecurityUserRegisteredEvent {
    String securityUserId;
    String name;
    Integer age;
    String sex;
    String phoneNum;
    String location;
}

@RabbitListener(queues = "sns-user-registered")
SnsUserRegisteredEvent {
    String securityUserId;
    String name;
    Integer age;
    String sex;
}
```

**Outbound Events (Published):**
```java
UserRegisteredEvent {
    String securityUserId;
    String userId;
}

UserRegisteredFailedEvent {
    String securityUserId;
}
```

### Service Dependencies
- **Security AOP**: Authentication context and user session management
- **Common Module**: UUID generation utilities and shared types
- **Message Module**: Event definitions and RabbitMQ integration

## 💼 Business Rules and Validations

### User Creation Rules
1. **Event-Driven Only**: Users can only be created through event consumption
2. **SecurityUser Link**: Every User must link to a SecurityUser
3. **Profile Completeness**: Basic profile information (name, age) is required
4. **Sex Enumeration**: Gender must be valid enum value (MALE, FEMALE, ETC)

### Data Consistency Rules
1. **Referential Integrity**: SecurityUserId must exist in Auth domain
2. **Unique Relationship**: One-to-one mapping between User and SecurityUser
3. **Audit Trail**: Creation and modification timestamps are maintained
4. **UUID Standards**: All identifiers use UUIDv7 format

### Error Handling Rules
1. **Compensation Transactions**: Failed registrations trigger cleanup events
2. **Event Retry**: RabbitMQ consumer retries on transient failures
3. **Orphan Prevention**: SecurityUser creation failures are handled gracefully

## 🎮 Usage Examples

### Event-Driven User Creation
```java
@RabbitListener(queues = "security-user-registered")
@Transactional
public void consume(SecurityUserRegisteredEvent event) {
    try {
        SecurityUserId securityUserId = new SecurityUserId(event.getSecurityUserId());
        UserId userId = userRegisterService.register(
            new RegisterRequest(securityUserId, event.getName(), event.getAge(),
                event.getSex(), event.getPhoneNum(), event.getLocation()));
        
        // Notify Auth service of successful creation
        rabbitTemplate.convertAndSend("user-registered",
            new UserRegisteredEvent(securityUserId.getValue(), userId.getValue()));
    } catch (Exception e) {
        // Trigger compensation transaction
        rabbitTemplate.convertAndSend("user-registered-failed", 
            new UserRegisteredFailedEvent(event.getSecurityUserId()));
    }
}
```

### User Profile Query
```java
@GetMapping("/me")
@AuthNeeded
@RoleRequired(value={Role.USER})
public ResponseEntity<RsData<UserInfo>> getMe() {
    UserSecurityToken userSecurityToken = LoggedInUserContext.get();
    UserInfo userinfo = userQueryService.findUser(userSecurityToken, 
        new UserId(userSecurityToken.getUserId()));
    return ResponseEntity.ok(new RsData<>("200", "User info", userinfo));
}
```

## 🏃 Running the Service

The User service is designed as a library module:

```bash
# Build the service
./gradlew :user:build

# Run tests
./gradlew :user:test

# The service is included as a library in other modules
# It doesn't run standalone (bootJar disabled)
```

## 🔄 Event-Driven Architecture

### Registration Flow

**Email Registration:**
```
1. Auth Service: User registers → SecurityUserRegisteredEvent
2. User Service: Consumes event → Creates User profile → UserRegisteredEvent  
3. Auth Service: Consumes event → Links User ID to SecurityUser
```

**Social Registration:**
```
1. Auth Service: Social login → SnsUserRegisteredEvent
2. User Service: Consumes event → Creates User profile → UserRegisteredEvent
3. Auth Service: Consumes event → Links User ID to SecurityUser
```

**Error Handling:**
```
1. User Service: Registration fails → UserRegisteredFailedEvent
2. Auth Service: Consumes event → Cleanup SecurityUser (future implementation)
```

### Event Processing Guarantees
- **At-least-once delivery**: RabbitMQ ensures message delivery
- **Transactional integrity**: Database operations are transactional
- **Compensating actions**: Failed registrations trigger cleanup events
- **Idempotent operations**: Event processing handles duplicates gracefully

## 🔍 Monitoring and Observability

### Key Metrics to Monitor
- User registration success/failure rates
- Event processing latency
- Database query performance
- Event consumption lag
- Profile completion rates

### Logging Points
- Event consumption start/success/failure
- User profile creation attempts
- Database transaction commits/rollbacks
- Compensation transaction triggers
- Authentication context usage

## ⚠️ Important Considerations

### Data Privacy
- Personal information (phone, location) requires proper handling
- GDPR compliance considerations for user data
- Secure storage and access patterns
- Data retention and deletion policies

### Event Ordering
- Events may arrive out of order
- Idempotent processing prevents duplicate users
- State consistency maintained through transactional boundaries

### Scalability
- Event-driven architecture supports horizontal scaling
- Database read/write separation potential
- RabbitMQ consumer groups for load distribution
- Stateless service design

### Error Recovery
- Dead letter queues for failed events
- Manual intervention capabilities for data consistency
- Monitoring and alerting for failed registrations
- Compensation transaction logging and tracking

---

The User Service provides a clean separation between authentication and user profile concerns while maintaining strong consistency through event-driven architecture and compensating transactions.
