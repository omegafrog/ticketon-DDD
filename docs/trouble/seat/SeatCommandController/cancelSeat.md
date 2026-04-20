# DELETE /api/v1/events/{event-id}/seats Trouble

## Before

- seat은 조회만 하는 것처럼 보여도 queue token과 availability가 같이 움직인다.
- 선점/취소/조회의 경계가 흐리면 중복 선택이 생긴다.
- 좌석 수와 현재 선택 수는 항상 동기화 관점에서 봐야 한다.

## Decision Points

- availability는 읽기, 선택은 상태 변경으로 분리한다.
- 대기열 토큰이 필요한 경로는 내부 검증과 함께 본다.
- 취소는 반드시 선점 해제와 함께 처리한다.

## Failure Modes

- 동시 선택은 double booking을 만든다.
- 취소 누락은 seat leak이 된다.
- 조회 결과가 stale하면 사용자 선택 UX가 깨진다.

## Why It Matters

- seat의 작은 race condition은 바로 매출 손실로 이어진다.

## Recent History

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)



## Related Docs

- [Use Case](../../usecase/seat/SeatCommandController/cancelSeat.md)
- [Flow](../../flow/seat/SeatCommandController/cancelSeat.md)
- [Troubleshooting](../../troubleshooting/seat/SeatCommandController/cancelSeat.md)
