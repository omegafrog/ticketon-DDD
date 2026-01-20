# Event Service

The Event Service is responsible for managing event creation, updates, and queries in the Ticketon system. It handles event lifecycle management, category validation, seat layout integration, and provides both command and query operations for events.

## 🎯 Purpose and Responsibilities

- **Event Management**: Create, update, and delete events with comprehensive validation
- **Event Queries**: Provide filtered and paginated event listings and detailed event information
- **Status Management**: Handle event status transitions (CLOSED, OPEN, etc.)
- **Integration**: Coordinate with seat layout service and category service
- **Authorization**: Ensure only authorized managers can modify events
- **Event Publishing**: Publish domain events for inter-service communication

## 🏗️ Architecture

### Domain Structure
```
event/
├── domain/                 # Core business entities and rules
│   ├── Event.java         # Root aggregate
│   ├── EventId.java       # Value object (UUID-based)
│   ├── EventInformation.java # Event details value object
│   ├── EventStatus.java   # Status enumeration
│   ├── ManagerId.java     # Manager identifier
│   └── MetaData.java      # Technical metadata
├── application/            # Application services
│   ├── RegisterEventService.java
│   ├── UpdateEventService.java
│   ├── FindEventService.java
│   └── EventQueryService.java
├── ui/                    # REST controllers
│   ├── EventController.java      # Command operations
│   └── EventQueryController.java # Query operations
├── infra/                 # Infrastructure layer
│   ├── EventRepositoryImpl.java
│   ├── JpaEventRepository.java
│   └── EventSpecification.java
└── global/                # DTOs and request/response objects
    ├── NewEventRequest.java
    ├── UpdateEventRequest.java
    ├── EventInfoResponse.java
    └── EventListResponse.java
```

### Key Domain Concepts

**Event Aggregate**
- Root entity containing all event information
- Enforces business rules and invariants
- Uses UUIDv7 for identifiers
- Embeds EventInformation and MetaData

**EventInformation Value Object**
- Contains user-facing event details
- Handles validation of dates, pricing, and content
- Supports status transitions and updates

## 🔌 API Endpoints

### Command Operations (EventController)
```
POST   /api/v1/events                    # Create new event
PUT    /api/v1/events/{eventId}          # Update event
DELETE /api/v1/events/{eventId}          # Delete event
PATCH  /api/v1/events/{eventId}          # Change event status
```

### Query Operations (EventQueryController)
```
POST   /api/v1/events/list               # Get filtered event list
GET    /api/v1/events/{id}               # Get event details
```

### Authentication & Authorization
All command operations require:
- `@AuthNeeded` - User must be authenticated
- `@RoleRequired(Role.MANAGER)` or `@RoleRequired({Role.MANAGER, Role.ADMIN})`

### Request/Response Examples

**Create Event Request:**
```json
{
  "title": "Concert Event",
  "thumbnailUrl": "https://example.com/thumb.jpg",
  "description": "Amazing concert event",
  "bookingStart": "2024-01-01T10:00:00",
  "bookingEnd": "2024-01-15T23:59:59",
  "startDate": "2024-01-20T19:00:00",
  "endDate": "2024-01-20T22:00:00",
  "minPrice": 50000,
  "maxPrice": 150000,
  "seatSelectable": true,
  "ageLimit": 18,
  "categoryId": "uuid-category-id",
  "seatLayout": { /* seat layout data */ }
}
```

**Event List Filter:**
```json
{
  "categoryIds": ["cat1", "cat2"],
  "minPrice": 10000,
  "maxPrice": 100000,
  "status": "OPEN",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59"
}
```

## 🔧 Configuration

### Dependencies (build.gradle)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    implementation project(':seat')        # Seat layout integration
    // category is now part of event module
    implementation project(':common')      # Shared utilities
    implementation project(':security-aop') # Authentication/authorization
    implementation project(':message')     # Domain events
}
```

### Application Configuration
```yaml
# application.yml
spring:
  application:
    name: event-service
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

## 🔗 Integration Points

### Internal Service Dependencies
- **Seat Service**: Validates and creates seat layouts during event creation
- **Category Service**: Validates event category existence
- **Security AOP**: Provides authentication and authorization
- **Message Module**: Publishes EventCreatedEvent for inter-service communication

### Domain Events Published
```java
EventCreatedEvent {
    String eventId;
    String title; 
    String managerId;
    Long seatLayoutId;
    Boolean seatSelectable;
    String locationName;
    String eventStart;
    String eventEnd;
}
```

## 💼 Business Rules and Validations

### Event Creation Rules
1. **Category Validation**: Event category must exist
2. **Date Validation**: 
   - Booking dates must be in the future
   - Event dates must be after booking dates
   - Start dates must be before end dates
3. **Price Validation**: Min/max price constraints
4. **Manager Authorization**: Only managers can create events

### Event Update Rules
1. **Ownership**: Only the event creator (manager) can update
2. **Timing Constraints**: Cannot update after booking period ends
3. **Status Transitions**: Managed through separate endpoint

### Event Deletion Rules
1. **Soft Delete**: Events are marked as deleted, not physically removed
2. **Authorization**: Only creator or admin can delete
3. **Timing**: Deletion allowed before booking ends

## 🎮 Usage Examples

### Creating an Event
```java
@AuthNeeded
@RoleRequired(Role.MANAGER)
public ResponseEntity<RsData<EventId>> eventRegister(@RequestBody NewEventRequest request) {
    EventId eventId = registerEventService.registerNewEvent(request);
    return ResponseEntity.ok(new RsData<>("200", "이벤트 등록 성공", eventId));
}
```

### Querying Events with Filters
```java
public ResponseEntity<RsData<Page<EventListResponse>>> getEvents(
    @RequestParam String keyword,
    @RequestBody EventListFilter filter, 
    Pageable pageable) {
    
    Page<EventListResponse> eventList = eventQueryService.getEvents(keyword, filter, pageable);
    return ResponseEntity.ok(new RsData<>("200", "event list 조회 성공", eventList));
}
```

## 🏃 Running the Service

The Event service is typically run as part of the larger microservices ecosystem:

```bash
# Build the service
./gradlew :event:build

# Run tests
./gradlew :event:test

# The service is included as a library in other modules
# It doesn't run standalone (bootJar disabled in build.gradle)
```

## 🔍 Monitoring and Observability

### Key Metrics to Monitor
- Event creation rates
- Query response times
- Validation failure rates
- Authorization denials
- Database query performance

### Logging Points
- Event creation with manager ID
- Update attempts and authorization checks
- Query performance and filter usage
- Domain event publishing success/failure

---

The Event Service is a core component of the Ticketon system, providing robust event management capabilities with strong business rule enforcement and seamless integration with other services.
