# POST /api/v1/auth/refresh

- Controller: `SecurityController.refreshTokens()`
- Actor: 외부 호출자
- Goal: 사용자는 만료된 토큰을 재발급받고 싶다.
- Source: `/mnt/e/workspace/ticketon-DDD/auth/src/main/java/org/codenbug/auth/ui/SecurityController.java`

## Use Case

사용자는 만료된 토큰을 재발급받고 싶다.

## Success Criteria

- 요청은 `POST` `/api/v1/auth/refresh` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<String>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/auth/SecurityController/refreshTokens.md)
- [Trouble](../../trouble/auth/SecurityController/refreshTokens.md)
- [Troubleshooting](../../troubleshooting/auth/SecurityController/refreshTokens.md)
