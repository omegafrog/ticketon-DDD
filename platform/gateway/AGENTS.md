# Platform Gateway

Purpose: Spring Cloud Gateway; all client traffic enters here.

Look here:
- Entry point: `platform/gateway/src/main/java/org/codenbug/gateway/GatewayApplication.java`
- Filters: `platform/gateway/src/main/java/org/codenbug/gateway/filter/`
- Base config: `platform/gateway/src/main/resources/application.yml`
- Prod routes: `platform/gateway/src/main/resources/application-gateway-routes-prod.yml`
- Whitelists: `platform/gateway/src/main/resources/application-whitelist-*.yml`

Rules:
- Route changes must consider whitelist enforcement.
- Default filters apply broadly; auth bypass must be explicit.
- Keep prod routing service-discovery friendly, usually `lb://...`.

Commands:
- `./gradlew :platform:gateway:test`
- `./gradlew :platform:gateway:bootRun`
