# =================================================================
# Stage 1: Build the application using Gradle
# =================================================================
FROM gradle:8.5-jdk21-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the entire project to build all dependencies
# This is necessary because the 'broker' module may depend on 'common', etc.
COPY . .

RUN chmod +x ./gradlew


# Build the 'broker' module, skipping tests for a faster build.
# The --no-daemon option is recommended for CI/CD and containerized environments.
RUN ./gradlew :dispatcher:build -x test --no-daemon

# =================================================================
# Stage 2: Create the final, minimal production image
# =================================================================
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/dispatcher/build/libs/*.jar app.jar


# Command to run the application
ENTRYPOINT ["java", "-jar", "-Dspring.config.location=file:/app/config/application.yml", "app.jar"]
