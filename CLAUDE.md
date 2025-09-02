# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a microservices-based ticket booking system built with Domain-Driven Design (DDD) principles using Spring Boot and Java 21. The system handles event management, user authentication, payment processing, seat management, and a queue system for high-traffic scenarios.

## Build System & Commands

**Primary Build Tool:** Gradle with multi-module setup

### Common Development Commands
```bash
# Build all modules
./gradlew build

# Run a specific service (replace SERVICE_NAME with actual module name)
./gradlew :SERVICE_NAME:bootRun

# Run tests for all modules
./gradlew test

# Build Docker images for services
./gradlew :SERVICE_NAME:bootBuildImage

# Start infrastructure services
docker-compose -f docker/docker-compose.yml up -d
```

### Service Ports
- Gateway: 8080 (main entry point)
- Eureka: 8761 (service discovery)
- Auth: 9001
- App: 9000
- MySQL: 3306
- Redis: 6379
- Kafka: 29092 (external), 19092 (internal)

## Architecture Overview

### Microservices Structure
The system follows a multi-module Gradle project structure with these core services:

**Core Business Services:**
- `auth` - Authentication/authorization with OAuth integration (Google, Kakao)
- `user` - User profile management
- `event` - Event creation, management, and querying
- `seat` - Seat layout and availability management
- `purchase` - Payment processing and ticket purchasing
- `broker` - SSE connections and waiting queue management
- `dispatcher` - Redis-based queue promotion with multithreading

**Infrastructure Services:**
- `gateway` - API Gateway with Spring Cloud Gateway (port 8080)
- `eureka` - Service discovery server
- `app` - Main application orchestrator (IMPORTS ONLY - no business logic or infrastructure code)

**Shared Modules:**
- `common` - Shared utilities, exceptions, and Redis services
- `message` - Event messages for inter-service communication
- `security-aop` - AOP-based security annotations and user context
- `category-id` - Event category management

### Domain Architecture
Every Domain module's app package does not contain any class of web(ServletRequet, resp, etc...)
Based on DDD principles with clear aggregate boundaries:
- **Event Domain**: Events, seats, layouts, pricing
- **Payment Domain**: Purchases, tickets, payment integration
- **Auth Domain**: Security users, social login providers
- **User Domain**: User profiles and information
- **Queue Domain**: Waiting queue and entry management

### Communication Patterns
- **Synchronous**: REST APIs through Gateway
- **Asynchronous**: Kafka for event-driven communication
- **Real-time**: SSE for queue notifications
- **Caching/State**: Redis for distributed locking and session management

### Key Technologies
- **Framework**: Spring Boot 3.5, Spring Cloud Gateway
- **Database**: MySQL with JPA/Hibernate
- **Message Broker**: Kafka (Bitnami)
- **Cache/Queue**: Redis
- **Service Discovery**: Eureka
- **Payment Integration**: Toss Payments API
- **Security**: JWT tokens, OAuth2 (Google/Kakao)

## Development Notes

### Testing
- Each module has its own test suite
- Look for `*ApplicationTests.java` files for integration tests
- No centralized test script - run tests per module

### Configuration Management
- Environment-specific configs: `application-{env}.yml`
- Secret configs: `application-secret.yml` (gitignored)
- Service discovery through Eureka for internal communication

### Queue System
The dispatcher module implements a sophisticated multithreaded promotion system:
- Uses Redis for temporary task lists
- Lua scripts for atomic operations
- Thread pool for parallel processing
- Distributed locking for seat management

### Payment Flow
- Purchase service initiates payments
- Worker module (if implemented) handles payment confirmations
- Event-driven updates across services
- Redis locks prevent overselling

## App Module Guidelines

**CRITICAL**: The `app` module serves ONLY as an application orchestrator that imports configurations from other modules. 

### What the App Module SHOULD NOT Contain:
- **Business Logic**: No domain services, application services, or business rules
- **Infrastructure Code**: No database configurations, message brokers, external API clients
- **Web Components**: No controllers, REST endpoints, or web-specific logic
- **Data Access**: No repositories, JPA configurations, or database-related code

### What the App Module SHOULD Contain:
- **Configuration Imports**: `@Import` annotations to bring in configurations from business modules
- **Application Entry Point**: `@SpringBootApplication` main class
- **Cross-cutting Concerns**: Only application-wide configurations like OpenAPI, Jackson, static resources

### Implementation Pattern:
```java
@Configuration  
@Import({
    UserConfig.class,           // Business module config
    UserDatabaseConfig.class,   // Import datasource beans
    EventConfig.class,          // Business module config  
    EventDatabaseConfig.class   // Import datasource beans
})
public class AppConfig {
    // No bean definitions here - only imports
}
```

Each business module should define its own:
- Database configurations (primary/readonly datasources)
- JPA configurations and entity scanning
- Repository configurations
- Service beans
- Message broker configurations

