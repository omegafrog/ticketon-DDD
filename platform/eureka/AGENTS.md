# Platform Eureka

Purpose: Eureka service discovery server.

Look here:
- Entry point: `platform/eureka/src/main/java/org/codenbug/eureka/EurekaApplication.java`
- Config: `platform/eureka/src/main/resources/application.yml`

Rules:
- Keep this module minimal and config-focused.
- Start Eureka before services that register locally.
- Do not use Eureka as config store or business layer.

Command:
- `./gradlew :platform:eureka:bootRun`
