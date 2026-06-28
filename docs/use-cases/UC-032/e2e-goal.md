# UC-032 E2E Goal

## Metadata
|Item|Value|
|---|---|
|Approval Status|approved|
|Approved by|user-confirmed requirements and use-case harvest|

## Goal
- Verify that an authenticated Notification Sender with ADMIN or MANAGER authority can create one unread Notification for a specified Notification Recipient.

## Business Success Criteria
- Valid create request persists exactly one Notification in Unread state for the specified Recipient User ID.
- Persisted Notification becomes observable in the target Notification Recipient's Notification Inbox.

## Business Failure Criteria
- Unauthenticated create request is rejected.
- USER role create request is rejected.
- Blank Recipient User ID or missing required input fails without persisting a Notification.

## Observability Boundary
- Browser-visible UI: no
- API/runtime observable behavior: yes
- Required user-visible evidence:
  Successful create response for Notification Sender and subsequent Notification visibility in the target Notification Recipient's Notification Inbox.

## Given
- An authenticated Notification Sender with ADMIN or MANAGER authority is available.
- A target Notification Recipient identifier and valid Notification payload are available.

## When
- Notification Sender submits a create request with Recipient User ID, Notification type, title, content, and optional target URL through the authenticated API.

## Then
- The system persists exactly one Notification in Unread state for the specified Notification Recipient.
- The target Notification Recipient can later observe that Notification in their Notification Inbox.
- Requests without authority or required input fail without persisting a Notification.

## Out of Scope
- Implementation-specific commands, fixtures, API request/response examples, UI automation steps, and actual pass/fail output.
- These details belong in `docs/plans/active/UC-032/verification.md` or the plan verification result after implementation.

## Needs Confirmation
- None
