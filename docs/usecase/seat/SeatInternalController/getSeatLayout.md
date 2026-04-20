# GET /internal/seat-layouts/{layout-id}

- Controller: `SeatInternalController.getSeatLayout()`
- Actor: 내부 서비스
- Goal: 좌석 조회/선택/취소를 대기열 토큰과 함께 안전하게 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/seat/src/main/java/org/codenbug/seat/ui/SeatInternalController.java`

## Use Case

좌석 조회/선택/취소를 대기열 토큰과 함께 안전하게 처리한다.

## Success Criteria

- 요청은 `GET` `/internal/seat-layouts/{layout-id}` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<SeatLayoutResponse>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/seat/SeatInternalController/getSeatLayout.md)
- [Trouble](../../trouble/seat/SeatInternalController/getSeatLayout.md)
- [Troubleshooting](../../troubleshooting/seat/SeatInternalController/getSeatLayout.md)
