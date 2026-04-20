# POST /api/v1/auth/refresh Flow

## Entry Point

- `SecurityController.refreshTokens()`
- `POST /api/v1/auth/refresh`

## Flow

- `SecurityController.refreshTokens()`가 `POST /api/v1/auth/refresh`를 처리한다.
- 컨트롤러는 입력 검증과 서비스 위임만 담당하고, 핵심 상태 변경은 application/service 계층으로 넘긴다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/auth/SecurityController/refreshTokens.md)
- [Trouble](../../trouble/auth/SecurityController/refreshTokens.md)
- [Troubleshooting](../../troubleshooting/auth/SecurityController/refreshTokens.md)
