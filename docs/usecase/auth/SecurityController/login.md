# POST /api/v1/auth/login

- Controller: `SecurityController.login()`
- Actor: 외부 호출자
- Goal: 사용자는 이메일/비밀번호로 로그인하고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/auth/src/main/java/org/codenbug/auth/ui/SecurityController.java`

## Use Case

사용자는 이메일/비밀번호로 로그인하고 싶다.

## Success Criteria

- 요청은 `POST` `/api/v1/auth/login` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<String>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/auth/SecurityController/login.md)
- [Trouble](../../trouble/auth/SecurityController/login.md)
- [Troubleshooting](../../troubleshooting/auth/SecurityController/login.md)
