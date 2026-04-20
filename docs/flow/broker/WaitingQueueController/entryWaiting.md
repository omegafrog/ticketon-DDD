# GET /api/v1/broker/events/{id}/tickets/waiting Flow

## Entry Point

- `WaitingQueueController.entryWaiting()`
- `GET /api/v1/broker/events/{id}/tickets/waiting`

## Flow

- 사용자가 `GET /api/v1/broker/events/{id}/tickets/waiting`로 진입한다.
- `WaitingQueueEntryService.entry()`가 SSE emitter를 만들고 Redis 큐 상태를 저장한다.
- dispatcher가 승격 슬롯을 계산해 `ENTRY` stream에 메시지를 넣는다.
- broker가 `DISPATCH`를 받아 승격 이벤트와 entry token을 SSE로 보낸다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/broker/WaitingQueueController/entryWaiting.md)
- [Trouble](../../trouble/broker/WaitingQueueController/entryWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/WaitingQueueController/entryWaiting.md)
