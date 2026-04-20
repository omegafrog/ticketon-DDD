# GET /api/v1/broker/polling/events/{id}/waiting

- Controller: `PollingWaitingQueueController.enterWaiting()`
- Actor: 외부 호출자
- Goal: 사용자는 polling 기반 대기열에 들어가고, 이후 순번 조회로 상태를 확인하고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/broker/src/main/java/org/codenbug/broker/ui/PollingWaitingQueueController.java`

## Use Case

사용자는 polling 기반 대기열에 들어가고, 이후 순번 조회로 상태를 확인하고 싶다.

## Success Criteria

- 요청은 `GET` `/api/v1/broker/polling/events/{id}/waiting` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Void>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/broker/PollingWaitingQueueController/enterWaiting.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/enterWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/enterWaiting.md)
