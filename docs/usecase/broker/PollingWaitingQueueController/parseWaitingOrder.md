# GET /api/v1/broker/polling/events/{id}/current

- Controller: `PollingWaitingQueueController.parseWaitingOrder()`
- Actor: 외부 호출자
- Goal: 사용자는 현재 대기열 순번, 입장 토큰, 다음 poll 간격을 알고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/broker/src/main/java/org/codenbug/broker/ui/PollingWaitingQueueController.java`

## Use Case

사용자는 현재 대기열 순번, 입장 토큰, 다음 poll 간격을 알고 싶다.

## Success Criteria

- 요청은 `GET` `/api/v1/broker/polling/events/{id}/current` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<PollingQueueInfo>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/parseWaitingOrder.md)
