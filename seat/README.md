# Seat Service

The Seat Service manages seat layouts, seat selection, and seat availability for events in the Ticketon system. It provides comprehensive seat management capabilities including layout creation, seat reservation with distributed locking, and real-time seat availability updates.

## 🎯 Purpose and Responsibilities

- **Seat Layout Management**: Create and update seat layouts for venues
- **Seat Selection**: Handle seat reservation and cancellation with concurrency control
- **Availability Tracking**: Real-time seat availability management
- **Distributed Locking**: Prevent seat double-booking using Redis locks
- **Event Integration**: Consume purchase events to update seat status
- **Entry Token Validation**: Ensure users have valid queue entry tokens

## 🏗️ Architecture

### Domain Structure
```
seat/
├── domain/                    # Core business entities
│   ├── SeatLayout.java       # Root aggregate for seat arrangements
│   ├── Seat.java             # Individual seat entity
│   ├── SeatId.java           # Value object (UUID-based)
│   ├── Location.java         # Venue location information
│   └── SeatLayoutRepository.java
├── app/                      # Application services
│   ├── RegisterSeatLayoutService.java
│   ├── FindSeatLayoutService.java
│   ├── UpdateSeatLayoutService.java
│   ├── SeatTransactionService.java
│   └── EventProjectionRepository.java
├── ui/                       # REST controllers
│   └── SeatController.java   # Seat operations API
├── infra/                    # Infrastructure layer
│   ├── SeatLayoutRepositoryImpl.java
│   ├── JpaSeatRepository.java
│   ├── EventProjectionRepositoryImpl.java
│   └── SeatPurchasedEventConsumer.java
├── global/                   # DTOs and configurations
│   ├── SeatLayoutResponse.java
│   ├── SeatSelectRequest.java
│   ├── SeatCancelRequest.java
│   └── RegisterSeatLayoutDto.java
└── query/model/              # Query projections
    └── EventProjection.java
```

### Key Domain Concepts

**SeatLayout Aggregate**
- Root entity managing seat arrangements
- Contains seat grid layout and location information
- Validates layout consistency with seat definitions
- Supports dynamic layout updates

**Seat Entity**
- Individual seat with signature, grade, and pricing
- Availability state management
- Reservation and cancellation operations
- UUID-based identification

**Location Value Object**
- Venue and hall name information
- Embedded in SeatLayout

## 🔌 API Endpoints

### Seat Operations
```
GET    /api/v1/events/{event-id}/seats     # Get seat layout
POST   /api/v1/events/{event-id}/seats     # Select seats
DELETE /api/v1/events/{event-id}/seats     # Cancel seat selection
```

### Authentication & Authorization
- **GET**: Public access for seat layout viewing
- **POST**: Requires `@AuthNeeded` and `@RoleRequired(Role.USER)`
- **DELETE**: Requires authentication and entry token validation
- All operations validate entry tokens for queue management

### Request/Response Examples

**Seat Layout Response:**
```json
{
  "id": 1,
  "layout": "[\n[\"A1\", \"A2\", \"A3\"],\n[\"B1\", \"B2\", \"B3\"]\n]",
  "seats": [
    {
      "id": "seat-uuid",
      "signature": "A1",
      "grade": "VIP",
      "price": 150000,
      "available": true
    }
  ],
  "hallName": "Main Hall",
  "locationName": "Seoul Arts Center"
}
```

**Seat Selection Request:**
```json
{
  "seatIds": ["seat-uuid-1", "seat-uuid-2"],
  "userId": "user-uuid"
}
```

**Seat Selection Response:**
```json
{
  "selectedSeats": [
    {
      "seatId": "seat-uuid-1",
      "signature": "A1",
      "price": 150000,
      "grade": "VIP"
    }
  ],
  "totalAmount": 150000,
  "reservationTime": "2024-01-20T14:30:00"
}
```

## 🔧 Configuration

### Dependencies (build.gradle)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.kafka:spring-kafka'
    
    implementation project(':message')      # Domain events
    implementation project(':common')       # Redis locks and utilities
    implementation project(':security-aop') # Authentication/authorization
}
```

### Application Configuration
```properties
# application.properties
spring.application.name=seat-service
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Kafka configuration for event consumption
spring.kafka.consumer.group-id=seat-service
```

## 🔗 Integration Points

### Internal Service Dependencies
- **Common Module**: Redis distributed locking, utilities
- **Security AOP**: Authentication and user context
- **Message Module**: Domain event consumption

### Event Consumption
The service consumes purchase-related events via Kafka:
```java
@KafkaListener(topics = "seat-purchased-event")
public void handleSeatPurchasedEvent(SeatPurchasedEvent event) {
    // Update seat availability after purchase
}
```

### Redis Lock Integration
Prevents concurrent seat selection:
```java
String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;
boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
```

## 💼 Business Rules and Validations

### Seat Layout Rules
1. **Layout Consistency**: All seats in layout must have corresponding seat definitions
2. **Signature Uniqueness**: Each seat signature must be unique within a layout
3. **Grid Validation**: Layout grid structure must match seat locations

### Seat Selection Rules
1. **Availability Check**: Only available seats can be selected
2. **Concurrency Control**: Distributed locks prevent double-booking
3. **Entry Token Validation**: Users must have valid queue entry tokens
4. **Time Limits**: Seat reservations have time limits (5 minutes)

### Seat Cancellation Rules
1. **Ownership Verification**: Users can only cancel their own selections
2. **Lock Release**: Distributed locks are released on cancellation
3. **Availability Restoration**: Cancelled seats become available immediately

## 🎮 Usage Examples

### Creating Seat Layout
```java
public SeatLayoutResponse registerSeatLayout(RegisterSeatLayoutDto seatLayout) {
    SeatLayout layout = new SeatLayout(
        seatLayout.getLayout(),
        new Location(seatLayout.getLocation(), seatLayout.getHallName()),
        seatLayout.getSeats().stream()
            .map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
            .toList()
    );
    return seatLayoutRepository.save(layout);
}
```

### Selecting Seats with Locking
```java
@Transactional
public void reserveSeat(Seat seat, String userId, String eventId, String seatId) {
    String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;
    String lockValue = UUID.randomUUID().toString();
    
    boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
    if (!lockSuccess) {
        throw new IllegalStateException("이미 선택된 좌석이 있습니다.");
    }
    
    try {
        seat.reserve();
    } catch (Exception e) {
        redisLockService.unlock(lockKey, lockValue);
        seat.cancelReserve();
        throw e;
    }
}
```

## 🏃 Running the Service

The Seat service runs as part of the microservices ecosystem:

```bash
# Build the service
./gradlew :seat:build

# Run tests
./gradlew :seat:test

# The service is included as a library in other modules
# It doesn't run standalone (bootJar disabled)
```

## 🔄 Event-Driven Architecture

### Events Consumed
- **SeatPurchasedEvent**: Updates seat status after successful purchase
- **PaymentFailedEvent**: Releases reserved seats on payment failure

### Integration with Event Projections
The service maintains event projections for efficient querying:
```java
@Entity
public class EventProjection {
    private String eventId;
    private String title;
    private Long seatLayoutId;
    private Boolean seatSelectable;
    // ... other fields
}
```

## 🔍 Monitoring and Observability

### Key Metrics to Monitor
- Seat selection success/failure rates
- Lock acquisition times and failures
- Seat availability updates
- Event consumption lag
- Database query performance

### Logging Points
- Distributed lock operations
- Seat state changes
- Event consumption processing
- Validation failures
- Concurrency conflicts

## ⚠️ Important Considerations

### Concurrency Control
- All seat operations use distributed Redis locks
- Lock timeout is set to 5 minutes
- Automatic lock release on exceptions
- Deadlock prevention through consistent lock ordering

### Performance Optimization
- Seat layout data is cached for frequent access
- Bulk operations for seat updates
- Efficient queries for availability checks
- Event projection updates for fast reads

### Error Handling
- Graceful degradation on lock failures
- Automatic seat release on transaction failures
- Comprehensive validation error messages
- Retry mechanisms for event consumption

---

The Seat Service is a critical component ensuring reliable and fair seat allocation in high-concurrency scenarios while maintaining data consistency across the distributed system.