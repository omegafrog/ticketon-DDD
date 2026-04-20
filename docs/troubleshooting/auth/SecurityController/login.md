# POST /api/v1/auth/login Troubleshooting

## Current State

- token lifecycle와 callback flow가 분리되어야 한다.
- role guard는 컨트롤러 밖에서 적용된다.

## Verification

- login/register/refresh/callback마다 실패 기준이 다른지 확인한다.
- 재발급과 폐기 경계가 동시에 보존되는지 본다.

## Quantitative Notes

- token phases: `2` (issue / refresh)
- callback trust boundaries: `1` external provider

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `0ae509c` (2026-02-05): feat: 대기열 폴링 주기 최적화 및 중복 진입 방지 로직 추가
- [controller] `96e3084` (2026-01-21): feat: 이벤트 수정 로직 및 데이터 처리 방식 개선
- [controller] `bad91b2` (2026-01-21): feat: 사용자 등록 검증 로직 추가 및 예외 처리 개선
- [controller] `fb58fa7` (2025-10-27): feat: kafka를 rabbitmq로 변경해 회원가입 로직 작성



## Related Docs

- [Use Case](../../usecase/auth/SecurityController/login.md)
- [Flow](../../flow/auth/SecurityController/login.md)
- [Trouble](../../trouble/auth/SecurityController/login.md)
