# GET /api/v1/auth/logout

- Controller: `SecurityController.logout()`
- Actor: 외부 호출자
- Goal: 로그인 사용자는 세션을 종료하고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/auth/src/main/java/org/codenbug/auth/ui/SecurityController.java`

## Use Case

로그인 사용자는 세션을 종료하고 싶다.

## Success Criteria

- 요청은 `GET` `/api/v1/auth/logout` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Void>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/auth/SecurityController/logout.md)
- [Trouble](../../trouble/auth/SecurityController/logout.md)
- [Troubleshooting](../../troubleshooting/auth/SecurityController/logout.md)
