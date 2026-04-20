# PUT /api/v1/users/me

- Controller: `UserCommandController.updateMe()`
- Actor: 사용자
- Goal: 사용자 프로필과 등록 검증을 관리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/user/src/main/java/org/codenbug/user/ui/UserCommandController.java`

## Use Case

사용자 프로필과 등록 검증을 관리한다.

## Success Criteria

- 요청은 `PUT` `/api/v1/users/me` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<UserInfo>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/user/UserCommandController/updateMe.md)
- [Trouble](../../trouble/user/UserCommandController/updateMe.md)
- [Troubleshooting](../../troubleshooting/user/UserCommandController/updateMe.md)
