# Broker Module

Purpose: SSE connections and Redis-backed waiting queue.

Look here:
- Entry point: `broker/src/main/java/org/codenbug/broker/BrokerApplication.java`
- Controllers: `broker/src/main/java/org/codenbug/broker/ui/`
- Services: `broker/src/main/java/org/codenbug/broker/service/`
- Redis/infra: `broker/src/main/java/org/codenbug/broker/infra/`
- Polling config: `broker/src/main/resources/application-polling.yml`

Rules:
- Queue state is Redis-centric; prefer atomic operations and clear TTL semantics.
- Clean up SSE emitters/queue state on disconnect.

Commands:
- `./gradlew :broker:test`
- `./gradlew :broker:bootRun`
