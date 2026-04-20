# POST /api/v1/notifications/test

- Controller: `NotificationCommandController.createTestNotification()`
- Actor: 테스트
- Goal: 테스트 알림을 직접 생성해 알림 생성 경로를 수동 검증한다.
- Source: `/mnt/e/workspace/ticketon-DDD/notification/src/main/java/org/codenbug/notification/controller/NotificationCommandController.java`

## Use Case

테스트 알림을 직접 생성해 알림 생성 경로를 수동 검증한다.

## Success Criteria

- 요청은 `POST` `/api/v1/notifications/test` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<NotificationDto>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/notification/NotificationCommandController/createTestNotification.md)
- [Trouble](../../trouble/notification/NotificationCommandController/createTestNotification.md)
- [Troubleshooting](../../troubleshooting/notification/NotificationCommandController/createTestNotification.md)
