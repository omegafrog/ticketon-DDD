# 승격 Lua 스크립트 중간 실패 시 카운터 불일치

## 문제(과거)
- 승격 Lua 스크립트가 중간 오류 발생 시 `error()`로 종료되며 전체 실패로 처리됐다.
- Redis Lua는 트랜잭션 롤백을 지원하지 않으므로, 이미 반영된 승격은 되돌아가지 않는다.
- 결과적으로 실제 승격은 일부 진행됐는데, 스크립트 결과가 0이거나 반환되지 않아
  `promotionCounter`와 실제 승격 수가 불일치할 수 있었다.

## 원인(과거)
- `promote_all_waiting_for_event.lua`에서 파싱 실패/레코드 누락 시 `error()` 호출
- `EntryPromoter.executePromotionScript()`는 반환값 기준으로 카운터를 업데이트

## 해결(현재 코드 반영)
- 중간 오류 시 전체 중단 대신, 해당 아이템만 스킵하고 다음 아이템을 처리한다.
- 검증 실패 항목은 waiting ZSET 및 관련 해시를 정리하고 진행한다.
- 스크립트는 실제 승격된 수(`cnt`)만 반환하여 카운터와 일치하도록 한다.

## 변경 사항
- `dispatcher/src/main/resources/promote_all_waiting_for_event.lua`
  - `error()` 제거 → `pcall` 실패 시 아이템 스킵
  - 실제 승격 수(`cnt`) 반환
