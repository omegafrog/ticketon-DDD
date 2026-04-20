# DELETE /api/v1/notifications/all

- Controller: `NotificationCommandController.deleteAllNotifications()`
- Actor: 관리자
- Goal: 알림 생성, 조회, 구독, 삭제를 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/notification/src/main/java/org/codenbug/notification/controller/NotificationCommandController.java`

## Use Case

알림 생성, 조회, 구독, 삭제를 처리한다.

## Success Criteria

- 요청은 `DELETE` `/api/v1/notifications/all` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Void>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/notification/NotificationCommandController/deleteAllNotifications.md)
- [Trouble](../../trouble/notification/NotificationCommandController/deleteAllNotifications.md)
- [Troubleshooting](../../troubleshooting/notification/NotificationCommandController/deleteAllNotifications.md)
