# GET /static/events/images/{fileName} Troubleshooting

## Current State

- 현재 응답 타입: `ResponseEntity<Resource>`
- 현재 접근 제어: auth=없음, roles=없음

## Verification

- controller contract는 실제 service 호출과 응답 타입을 기준으로 검증한다.
- 필요한 경우 integration test, targeted unit test, 또는 existing deep dive 문서로 보완한다.

## Quantitative Notes

- document sections: `3` (state, validation, numbers)

## Recent History

- [controller] `ca5516c` (2025-10-27): refactor: 코드 컨벤션에 맞게 탭을 스페이스로 변경
- [controller] `f3f43ba` (2025-08-20): feat: 이미지 업로드 시스템 구현



## Related Docs

- [Use Case](../../usecase/app/StaticController/getEventImage.md)
- [Flow](../../flow/app/StaticController/getEventImage.md)
- [Trouble](../../trouble/app/StaticController/getEventImage.md)
