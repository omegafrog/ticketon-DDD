# GET /static/events/images/{fileName} Flow

## Entry Point

- `StaticController.getEventImage()`
- `GET /static/events/images/{fileName}`

## Flow

- `StaticController.getEventImage()`가 `GET /static/events/images/{fileName}`를 처리한다.
- 컨트롤러는 입력 검증과 서비스 위임만 담당하고, 핵심 상태 변경은 application/service 계층으로 넘긴다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/app/StaticController/getEventImage.md)
- [Trouble](../../trouble/app/StaticController/getEventImage.md)
- [Troubleshooting](../../troubleshooting/app/StaticController/getEventImage.md)
