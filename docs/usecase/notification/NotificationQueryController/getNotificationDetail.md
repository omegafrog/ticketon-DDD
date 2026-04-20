# GET /api/v1/notifications/{id}

- Controller: `NotificationQueryController.getNotificationDetail()`
- Actor: 외부 호출자
- Goal: 알림 생성, 조회, 구독, 삭제를 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/notification/src/main/java/org/codenbug/notification/controller/NotificationQueryController.java`

## Use Case

알림 생성, 조회, 구독, 삭제를 처리한다.

## Success Criteria

- 요청은 `GET` `/api/v1/notifications/{id}` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<NotificationDto>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/notification/NotificationQueryController/getNotificationDetail.md)
- [Trouble](../../trouble/notification/NotificationQueryController/getNotificationDetail.md)
- [Troubleshooting](../../troubleshooting/notification/NotificationQueryController/getNotificationDetail.md)
