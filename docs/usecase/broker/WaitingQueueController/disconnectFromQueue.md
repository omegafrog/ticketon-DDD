# POST /api/v1/broker/events/{id}/tickets/disconnect

- Controller: `WaitingQueueController.disconnectFromQueue()`
- Actor: 외부 호출자
- Goal: 연결이 끊긴 사용자는 대기열 상태를 명시적으로 해제하고 다음 사용자 승격을 돕고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/broker/src/main/java/org/codenbug/broker/ui/WaitingQueueController.java`

## Use Case

연결이 끊긴 사용자는 대기열 상태를 명시적으로 해제하고 다음 사용자 승격을 돕고 싶다.

## Success Criteria

- 요청은 `POST` `/api/v1/broker/events/{id}/tickets/disconnect` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Void>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/broker/WaitingQueueController/disconnectFromQueue.md)
- [Trouble](../../trouble/broker/WaitingQueueController/disconnectFromQueue.md)
- [Troubleshooting](../../troubleshooting/broker/WaitingQueueController/disconnectFromQueue.md)
