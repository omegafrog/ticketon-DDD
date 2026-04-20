# GET /internal/events/{eventId}/version-check Flow

## Entry Point

- `EventInternalController.validateEventVersion()`
- `GET /internal/events/{eventId}/version-check`

## Flow

- `GET /internal/events/{eventId}/version-check`가 version/status를 받는다.
- `EventRepository.isVersionAndStatusValid()`로 현재 상태를 바로 조회한다.
- 결과는 `true/false`로 반환되고, 유효하지 않은 상태 문자열은 `400`으로 막힌다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/event/EventInternalController/validateEventVersion.md)
- [Trouble](../../trouble/event/EventInternalController/validateEventVersion.md)
- [Troubleshooting](../../troubleshooting/event/EventInternalController/validateEventVersion.md)
