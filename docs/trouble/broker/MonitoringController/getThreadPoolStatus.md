# GET /api/v1/monitoring/threadpool Trouble

## Before

- broker는 HTTP 응답과 queue 위치를 단일 요청-응답으로 끝낼 수 없어서 SSE와 Redis를 함께 써야 한다.
- 순번 보장은 단순 조회가 아니라 등록 순서, slot 수, 인스턴스 분산 처리까지 묶여 있다.
- 중복 진입 방지는 사용자 한 명 기준이 아니라 eventId + userId 경계로 봐야 한다.

## Decision Points

- SSE 연결을 먼저 확보하고, 그 다음 Redis에 queue state를 기록한다.
- 순번은 `WAITING_QUEUE_IDX`의 단조 증가값으로 고정하고, rank는 zset이 따르도록 만든다.
- entry stream은 인스턴스별 dispatch로 분리해 ack 책임을 좁힌다.

## Failure Modes

- 연결이 없는 상태에서 queue state만 남으면 승격 메시지가 유실된다.
- slot 계산이 틀리면 한꺼번에 너무 많은 사용자에게 입장을 허용할 수 있다.
- polling/dispatch가 꼬이면 pending 메시지가 누적되어 queue가 멈춘 것처럼 보인다.

## Why It Matters

- broker는 공정성과 처리량을 동시에 지켜야 하므로, 작은 상태 오염이 바로 사용자 체감 문제로 이어진다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `ca5516c` (2025-10-27): refactor: 코드 컨벤션에 맞게 탭을 스페이스로 변경
- [controller] `4833153` (2025-10-26): fix: 빌드 실패 해결



## Related Docs

- [Use Case](../../usecase/broker/MonitoringController/getThreadPoolStatus.md)
- [Flow](../../flow/broker/MonitoringController/getThreadPoolStatus.md)
- [Troubleshooting](../../troubleshooting/broker/MonitoringController/getThreadPoolStatus.md)
