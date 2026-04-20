# GET /api/v1/users/me

- Controller: `UserQueryController.getMe()`
- Actor: 사용자, 매니저
- Goal: 사용자 프로필과 등록 검증을 관리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/user/src/main/java/org/codenbug/user/ui/UserQueryController.java`

## Use Case

사용자 프로필과 등록 검증을 관리한다.

## Success Criteria

- 요청은 `GET` `/api/v1/users/me` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<UserInfo>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/user/UserQueryController/getMe.md)
- [Trouble](../../trouble/user/UserQueryController/getMe.md)
- [Troubleshooting](../../troubleshooting/user/UserQueryController/getMe.md)
