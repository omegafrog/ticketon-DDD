# GET /api/v1/broker/events/{id}/tickets/waiting

- Controller: `WaitingQueueController.entryWaiting()`
- Actor: 외부 호출자
- Goal: 사용자는 대기열에 진입하고 SSE로 순번과 승격 상태를 계속 받고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/broker/src/main/java/org/codenbug/broker/ui/WaitingQueueController.java`

## Use Case

사용자는 대기열에 진입하고 SSE로 순번과 승격 상태를 계속 받고 싶다.

## Success Criteria

- 요청은 `GET` `/api/v1/broker/events/{id}/tickets/waiting` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `SseEmitter` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/broker/WaitingQueueController/entryWaiting.md)
- [Trouble](../../trouble/broker/WaitingQueueController/entryWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/WaitingQueueController/entryWaiting.md)
