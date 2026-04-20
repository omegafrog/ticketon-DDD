# DELETE /api/v1/events/{event-id}/seats

- Controller: `SeatCommandController.cancelSeat()`
- Actor: 외부 호출자
- Goal: 좌석 조회/선택/취소를 대기열 토큰과 함께 안전하게 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/seat/src/main/java/org/codenbug/seat/ui/SeatCommandController.java`

## Use Case

좌석 조회/선택/취소를 대기열 토큰과 함께 안전하게 처리한다.

## Success Criteria

- 요청은 `DELETE` `/api/v1/events/{event-id}/seats` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Void>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/seat/SeatCommandController/cancelSeat.md)
- [Trouble](../../trouble/seat/SeatCommandController/cancelSeat.md)
- [Troubleshooting](../../troubleshooting/seat/SeatCommandController/cancelSeat.md)
