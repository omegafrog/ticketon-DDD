# POST /api/v1/notifications/batch-delete Trouble

## Before

- notification은 생성/조회/구독/삭제가 각각 delivery와 lifecycle 경계가 다르다.
- test notification은 실제 발송 경로와 분리해서 봐야 한다.
- 구독 관리가 느슨하면 알림 폭주나 누락이 생긴다.

## Decision Points

- 테스트 엔드포인트는 운영 발송과 분리한다.
- 구독/삭제는 상태 전이를 명확히 둔다.
- 조회는 delivery status를 직접 바꾸지 않는다.

## Failure Modes

- test path가 production channel을 건드리면 장애가 난다.
- 구독 상태가 꼬이면 알림이 중복되거나 누락된다.
- 삭제가 늦으면 stale subscription이 남는다.

## Why It Matters

- notification은 다른 서비스 결과를 사용자에게 전면 공개하므로, lifecycle이 곧 신뢰다.

## Recent History

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)



## Related Docs

- [Use Case](../../usecase/notification/NotificationCommandController/batchDeleteNotifications.md)
- [Flow](../../flow/notification/NotificationCommandController/batchDeleteNotifications.md)
- [Troubleshooting](../../troubleshooting/notification/NotificationCommandController/batchDeleteNotifications.md)
