# GET /api/v1/monitoring/threadpool/summary Troubleshooting

## Current State

- SSE connection lifecycle와 Redis cleanup이 같은 흐름에 있어야 한다.
- entry와 disconnect는 서로 다른 상태 전이를 가진다.

## Verification

- connection이 끊긴 뒤에도 slot/token/queue record가 남지 않는지 확인한다.
- pending stream message가 ACK되지 않고 쌓이지 않는지 본다.

## Quantitative Notes

- connection timeout: `0L`
- dispatch poll timeout: `1s`

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `ca5516c` (2025-10-27): refactor: 코드 컨벤션에 맞게 탭을 스페이스로 변경
- [controller] `4833153` (2025-10-26): fix: 빌드 실패 해결



## Related Docs

- [Use Case](../../usecase/broker/MonitoringController/getThreadPoolSummary.md)
- [Flow](../../flow/broker/MonitoringController/getThreadPoolSummary.md)
- [Trouble](../../trouble/broker/MonitoringController/getThreadPoolSummary.md)
