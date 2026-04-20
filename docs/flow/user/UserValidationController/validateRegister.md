# POST /internal/users/validate Flow

## Entry Point

- `UserValidationController.validateRegister()`
- `POST /internal/users/validate`

## Flow

- `UserValidationController.validateRegister()`가 `POST /internal/users/validate`를 처리한다.
- 내부 호출자는 인증/권한보다 내부 시스템 경계를 우선한다.
- 컨트롤러는 입력 검증과 서비스 위임만 담당하고, 핵심 상태 변경은 application/service 계층으로 넘긴다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/user/UserValidationController/validateRegister.md)
- [Trouble](../../trouble/user/UserValidationController/validateRegister.md)
- [Troubleshooting](../../troubleshooting/user/UserValidationController/validateRegister.md)
