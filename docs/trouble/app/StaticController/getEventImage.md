# GET /static/events/images/{fileName} Trouble

## Before

- 이 엔드포인트는 현재 구현 경계를 드러내는 usecase다.
- 과거에는 인증, 정합성, 외부 연계, 상태 전이가 더 넓게 섞여 있었을 가능성이 높다.
- 이 문서는 그 경계를 코드와 commit history로 다시 읽을 수 있게 만든다.

## Decision Points

- 컨트롤러는 가능한 얇게 유지하고, 실제 판단은 service/repository로 내린다.
- 입력 경계와 상태 경계를 분리한다.
- 역사적으로 바뀐 부분은 history section에 남긴다.

## Failure Modes

- 경계가 넓어지면 테스트와 rollback이 어려워진다.
- 책임이 섞이면 같은 문제를 여러 곳에서 다시 고치게 된다.
- 문서가 generic해지면 과거 의도를 다시 추적하기 어렵다.

## Why It Matters

- 결국 trouble 문서는 '왜 이 모양으로 수렴했는가'를 보여줘야 한다.

## Recent History

- [controller] `ca5516c` (2025-10-27): refactor: 코드 컨벤션에 맞게 탭을 스페이스로 변경
- [controller] `f3f43ba` (2025-08-20): feat: 이미지 업로드 시스템 구현



## Related Docs

- [Use Case](../../usecase/app/StaticController/getEventImage.md)
- [Flow](../../flow/app/StaticController/getEventImage.md)
- [Troubleshooting](../../troubleshooting/app/StaticController/getEventImage.md)
