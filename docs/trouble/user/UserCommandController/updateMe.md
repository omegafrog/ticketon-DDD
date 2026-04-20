# PUT /api/v1/users/me Trouble

## Before

- user profile과 registration check는 단순 조회처럼 보여도 unique/consistency 경계가 중요하다.
- 관리자/사용자 경계가 섞이면 조회 결과가 달라진다.
- profile 수정과 검증은 같은 user id라도 다른 정합성 조건을 가진다.

## Decision Points

- 조회와 수정의 책임을 분리한다.
- 등록 검증은 중복/존재 여부를 명시적으로 둔다.
- 권한에 따라 노출 필드를 좁힌다.

## Failure Modes

- 중복 등록 허용은 계정 충돌로 이어진다.
- 권한 누락은 개인정보 노출로 이어진다.
- profile update와 validation이 섞이면 rollback이 어려워진다.

## Why It Matters

- user는 다른 도메인의 actor이기도 해서, 작은 변경이 여러 흐름에 퍼진다.

## Recent History

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `a49d73c` (2025-09-10): refactor: 서비스 계층에서 독립된 redislock 모듈로 import 변경
- [controller] `69c7217` (2025-09-02): chore: QueryDSL 설정 및 사용자 서비스 ReadOnly Repository 패턴 적용
- [controller] `0ee3bc9` (2025-09-02): feat: 모든 컨트롤러에 Swagger API 문서화 어노테이션 추가



## Related Docs

- [Use Case](../../usecase/user/UserCommandController/updateMe.md)
- [Flow](../../flow/user/UserCommandController/updateMe.md)
- [Troubleshooting](../../troubleshooting/user/UserCommandController/updateMe.md)
