# GET /internal/events/{eventId}/version-check

- Controller: `EventInternalController.validateEventVersion()`
- Actor: purchase/seat
- Goal: purchase는 이벤트가 변경되지 않았는지를 내부적으로 빠르게 검증하고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/event/src/main/java/org/codenbug/event/ui/EventInternalController.java`

## Use Case

purchase는 이벤트가 변경되지 않았는지를 내부적으로 빠르게 검증하고 싶다.

## Success Criteria

- 요청은 `GET` `/internal/events/{eventId}/version-check` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Boolean>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/event/EventInternalController/validateEventVersion.md)
- [Trouble](../../trouble/event/EventInternalController/validateEventVersion.md)
- [Troubleshooting](../../troubleshooting/event/EventInternalController/validateEventVersion.md)
