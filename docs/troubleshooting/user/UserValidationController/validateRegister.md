# POST /internal/users/validate Troubleshooting

## Current State

- profile과 register-check는 조회성처럼 보여도 정합성 체크가 포함된다.

## Verification

- 권한별 노출이 다른지 확인한다.
- 중복/존재 체크가 update와 섞이지 않는지 본다.

## Quantitative Notes

- entity scope: `1 user`

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `bad91b2` (2026-01-21): feat: 사용자 등록 검증 로직 추가 및 예외 처리 개선



## Related Docs

- [Use Case](../../usecase/user/UserValidationController/validateRegister.md)
- [Flow](../../flow/user/UserValidationController/validateRegister.md)
- [Trouble](../../trouble/user/UserValidationController/validateRegister.md)
