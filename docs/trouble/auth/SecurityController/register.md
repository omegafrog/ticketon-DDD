# POST /api/v1/auth/register Trouble

## Before

- auth는 register/login/logout/refresh/social callback이 서로 다른 신뢰 경계를 가진다.
- 토큰 재발급과 소셜 callback은 replay와 redirect가 섞이기 쉽다.
- role guard는 컨트롤러 바깥에서 일관되게 적용돼야 한다.

## Decision Points

- login/register는 credential 검증과 발급 분리를 명확히 한다.
- refresh는 만료/재사용/폐기 경계가 중요하다.
- social callback은 외부 provider 응답을 직접 trust하지 않는다.

## Failure Modes

- callback replay는 잘못된 계정 연결을 만들 수 있다.
- refresh 토큰 재사용을 막지 못하면 세션 탈취가 길어진다.
- 권한 검사 누락은 관리 경계를 깨뜨린다.

## Why It Matters

- auth는 한 번 잘못되면 전체 시스템 신뢰가 무너진다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `0ae509c` (2026-02-05): feat: 대기열 폴링 주기 최적화 및 중복 진입 방지 로직 추가
- [controller] `96e3084` (2026-01-21): feat: 이벤트 수정 로직 및 데이터 처리 방식 개선
- [controller] `bad91b2` (2026-01-21): feat: 사용자 등록 검증 로직 추가 및 예외 처리 개선
- [controller] `fb58fa7` (2025-10-27): feat: kafka를 rabbitmq로 변경해 회원가입 로직 작성



## Related Docs

- [Use Case](../../usecase/auth/SecurityController/register.md)
- [Flow](../../flow/auth/SecurityController/register.md)
- [Troubleshooting](../../troubleshooting/auth/SecurityController/register.md)
