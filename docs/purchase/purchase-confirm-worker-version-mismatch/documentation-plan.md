# Documentation Plan

## Planned Planning Docs
- `plan.md`
  - task entry 문서와 상대 경로 링크 허브 역할
- `docs/purchase/purchase-confirm-worker-version-mismatch/domain-boundary.md`
  - task 범위와 비범위를 고정
- `docs/purchase/purchase-confirm-worker-version-mismatch/use-cases.md`
  - `UC-001` 단일 use case로 검증 시나리오 고정
- `docs/purchase/purchase-confirm-worker-version-mismatch/event-storming.md`
  - task 전용 command, event, policy traceability 제공
- `docs/purchase/purchase-confirm-worker-version-mismatch/detailed-design.md`
  - ports, adapters, interface signatures, key DTOs, test points 명시

## Oracle Preservation
- oracle의 목표, 대상 식별, 판단, 구현 계획, 테스트 설계 메모, 검증, 리스크를 위 분리 문서에 그대로 반영한다.
- oracle 원문과 plan-level 필수 섹션 보존 기준은 기존 상세 계획 문서에 둔다.
  - `docs/purchase/purchase-confirm-worker-version-mismatch-plan.md`

## Scope Control
- validator가 요구한 planning-doc completeness만 보완한다.
- `Implementation Plan`, `Verification Plan`, `Documentation Plan`, `Output Files`의 plan-level 필수 섹션은 `plan.md`에 유지한다.
- task scope 확장은 하지 않는다.
- 구현 코드, 테스트 코드, 일반 repo 문서는 변경하지 않는다.

## Verification Reference
- `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest`
