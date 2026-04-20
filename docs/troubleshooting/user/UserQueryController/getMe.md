# GET /api/v1/users/me Troubleshooting

## Current State

- profile과 register-check는 조회성처럼 보여도 정합성 체크가 포함된다.

## Verification

- 권한별 노출이 다른지 확인한다.
- 중복/존재 체크가 update와 섞이지 않는지 본다.

## Quantitative Notes

- entity scope: `1 user`

## Recent History

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `a49d73c` (2025-09-10): refactor: 서비스 계층에서 독립된 redislock 모듈로 import 변경
- [controller] `69c7217` (2025-09-02): chore: QueryDSL 설정 및 사용자 서비스 ReadOnly Repository 패턴 적용
- [controller] `0ee3bc9` (2025-09-02): feat: 모든 컨트롤러에 Swagger API 문서화 어노테이션 추가



## Related Docs

- [Use Case](../../usecase/user/UserQueryController/getMe.md)
- [Flow](../../flow/user/UserQueryController/getMe.md)
- [Trouble](../../trouble/user/UserQueryController/getMe.md)
