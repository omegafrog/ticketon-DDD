# DELETE /api/v1/broker/polling/events/{id}/waiting Flow

## Entry Point

- `PollingWaitingQueueController.disconnectWaiting()`
- `DELETE /api/v1/broker/polling/events/{id}/waiting`

## Flow

- `PollingWaitingQueueController.disconnectWaiting()`가 `DELETE /api/v1/broker/polling/events/{id}/waiting`를 처리한다.
- 서비스는 로그인 사용자의 entry token과 waiting record를 제거한다.
- 이벤트의 entry slot을 다시 증가시켜 다음 사용자가 들어갈 수 있게 만든다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/disconnectWaiting.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/disconnectWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/disconnectWaiting.md)
