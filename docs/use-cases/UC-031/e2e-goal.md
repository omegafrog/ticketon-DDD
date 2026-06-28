# UC-031 E2E Goal

## Metadata
|Item|Value|
|---|---|
|Approval Status|approved|
|Approved by|user-confirmed requirements and use-case harvest|

## Goal
- Verify that an authenticated Notification Recipient can delete owned Notifications by single, selected-set, and all-owned scopes while ownership isolation remains enforced.

## Business Success Criteria
- Single, selected-set, and all-owned deletion requests remove only owned Notifications in the requested scope.
- Selected deletion tolerates already missing owned Notifications and still deletes existing owned Notifications.

## Business Failure Criteria
- Unauthenticated deletion request is rejected.
- Deletion request that includes another recipient's Notification is rejected.

## Observability Boundary
- Browser-visible UI: no
- API/runtime observable behavior: yes
- Required user-visible evidence:
  Deletion result for requested scope and subsequent absence of deleted owned Notifications from the Notification Inbox.

## Given
- An authenticated Notification Recipient owns persisted Notifications and at least one deletion scenario exists for single, selected-set, and all-owned scope.

## When
- The Notification Recipient requests deletion for each supported scope through the gateway REST API.

## Then
- The system deletes only owned Notifications in the approved scope.
- A request containing another recipient's Notification is rejected.
- Selected deletion ignores already missing owned Notifications and still deletes the remaining existing owned Notifications.

## Out of Scope
- Implementation-specific commands, fixtures, API request/response examples, UI automation steps, and actual pass/fail output.
- These details belong in `docs/plans/active/UC-031/verification.md` or the plan verification result after implementation.

## Needs Confirmation
- None
