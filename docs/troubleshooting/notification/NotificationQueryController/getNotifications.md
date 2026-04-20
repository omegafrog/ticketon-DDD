# GET /api/v1/notifications Troubleshooting

## Current State

- test notification과 production delivery를 분리한다.

## Verification

- subscribe/delete가 stale state를 남기지 않는지 확인한다.

## Quantitative Notes

- delivery modes: `2+` (test / normal)

## Recent History

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)



## Related Docs

- [Use Case](../../usecase/notification/NotificationQueryController/getNotifications.md)
- [Flow](../../flow/notification/NotificationQueryController/getNotifications.md)
- [Trouble](../../trouble/notification/NotificationQueryController/getNotifications.md)
