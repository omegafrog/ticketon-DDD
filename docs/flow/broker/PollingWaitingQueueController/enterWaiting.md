# GET /api/v1/broker/polling/events/{id}/waiting Flow

## Entry Point

- `PollingWaitingQueueController.enterWaiting()`
- `GET /api/v1/broker/polling/events/{id}/waiting`

## Flow

- `PollingWaitingQueueController.enterWaiting()`가 `GET /api/v1/broker/polling/events/{id}/waiting`를 처리한다.
- 서비스는 로그인 사용자 기준으로 중복 entry token과 event 점유를 검사한다.
- 이벤트 좌석 수와 상태를 초기화한 뒤 waiting zset, last seen, 인스턴스 메타를 기록한다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/enterWaiting.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/enterWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/enterWaiting.md)
