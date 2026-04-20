# GET /api/v1/auth/social/{socialLoginType}/callback

- Controller: `SecurityController.callback()`
- Actor: 외부 호출자
- Goal: OAuth callback이 토큰 발급으로 이어지길 원한다.
- Source: `/mnt/e/workspace/ticketon-DDD/auth/src/main/java/org/codenbug/auth/ui/SecurityController.java`

## Use Case

OAuth callback이 토큰 발급으로 이어지길 원한다.

## Success Criteria

- 요청은 `GET` `/api/v1/auth/social/{socialLoginType}/callback` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<String>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/auth/SecurityController/callback.md)
- [Trouble](../../trouble/auth/SecurityController/callback.md)
- [Troubleshooting](../../troubleshooting/auth/SecurityController/callback.md)
