# POST /api/v1/broker/events/{id}/tickets/disconnect Flow

## Entry Point

- `WaitingQueueController.disconnectFromQueue()`
- `POST /api/v1/broker/events/{id}/tickets/disconnect`

## Flow

- 사용자가 `POST /api/v1/broker/events/{id}/tickets/disconnect`를 호출한다.
- `WaitingQueueEntryService.disconnect()`가 현재 상태를 보고 Redis 큐/토큰을 정리한다.
- IN_PROGRESS 상태면 승격 slot을 반환하고, 대기 중이면 waiting ZSET에서도 제거한다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/broker/WaitingQueueController/disconnectFromQueue.md)
- [Trouble](../../trouble/broker/WaitingQueueController/disconnectFromQueue.md)
- [Troubleshooting](../../troubleshooting/broker/WaitingQueueController/disconnectFromQueue.md)
