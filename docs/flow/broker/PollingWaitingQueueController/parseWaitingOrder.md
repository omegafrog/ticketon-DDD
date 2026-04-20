# GET /api/v1/broker/polling/events/{id}/current Flow

## Entry Point

- `PollingWaitingQueueController.parseWaitingOrder()`
- `GET /api/v1/broker/polling/events/{id}/current`

## Flow

- `PollingWaitingQueueController.parseWaitingOrder()`가 `GET /api/v1/broker/polling/events/{id}/current`를 처리한다.
- 서비스는 로그인 사용자의 queue event TTL을 갱신한다.
- entry token이 있으면 ENTRY 상태를, 아니면 zset rank를 읽어 WAITING 상태를 반환한다.
- poll-after-ms는 rank와 event 상태, entry slots, waiting size를 기준으로 조정한다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/parseWaitingOrder.md)
