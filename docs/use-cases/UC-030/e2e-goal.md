# UC-030 E2E Goal

## Metadata
|Item|Value|
|---|---|
|Approval Status|approved|
|Approved by|user-confirmed requirements and use-case harvest|

## Goal
- Verify that an authenticated Notification Recipient can view their Notification Inbox, unread subset, unread count, and Notification detail for their own Notifications only.

## Business Success Criteria
- Latest-first paginated Notification list is observable for the authenticated Notification Recipient.
- Unread list and unread count are observable without changing Notification state.
- Successful Notification detail retrieval changes only that Notification from Unread to Read.

## Business Failure Criteria
- Unauthenticated inbox access is rejected.
- Notification detail request for a non-existent or non-owned Notification is rejected without changing Notification state.

## Observability Boundary
- Browser-visible UI: no
- API/runtime observable behavior: yes
- Required user-visible evidence:
  Recipient-scoped list, unread list, unread count, detail response, and post-detail read-state change for only the retrieved Notification.

## Given
- An authenticated Notification Recipient has persisted Notifications in their Notification Inbox, including at least one Unread Notification.

## When
- The Notification Recipient requests the full Notification Inbox, unread list, unread count, and one Notification detail through the gateway REST API.

## Then
- The system returns only Notifications addressed to that Notification Recipient.
- The Notification list is latest-first and paginated.
- Unread list and unread count do not change Notification state.
- Successful detail retrieval returns the requested Notification and changes only that Notification from Unread to Read.

## Out of Scope
- Implementation-specific commands, fixtures, API request/response examples, UI automation steps, and actual pass/fail output.
- These details belong in `docs/plans/active/UC-030/verification.md` or the plan verification result after implementation.

## Needs Confirmation
- None
