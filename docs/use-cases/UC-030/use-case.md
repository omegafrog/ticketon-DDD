# UC-030. Notification Recipient views their Notification Inbox

## Actor
- Notification Recipient

## Supporting Actor
- None

## Goal
- Review Notifications addressed to their own Recipient User ID through the Notification Inbox.

## Preconditions
- The requester is authenticated.
- The Notification Inbox contains zero or more Notifications for the requester.

## Main Flow
1. Notification Recipient requests their Notification Inbox through the gateway REST API.
2. System returns the recipient-scoped Notification list in latest-first paginated order.
3. Notification Recipient optionally requests the Unread Notification list or Unread Notification count.
4. System returns the requested unread view without changing Read or Unread state.
5. Notification Recipient requests one Notification detail from their own Notification Inbox.
6. System returns the Notification detail and changes only that retrieved Notification from Unread to Read when detail retrieval succeeds.

## Failure Flow
- Unauthenticated request is rejected.
- Detail request for a non-existent Notification fails and does not change Read or Unread state.
- Detail request for a Notification addressed to another Recipient User ID fails and does not change Read or Unread state.

## Result
- Notification Recipient can observe their own Notification Inbox list, unread subset, unread count, and Notification detail.
- Successful detail retrieval updates only the retrieved Notification state.

## Observable Constraints From Requirements
- Recipient scope is limited to Notifications addressed to the authenticated requester.
- List response is latest-first and paginated.
- Unread list and unread count do not change Notification state.
- Successful detail retrieval changes only the retrieved Notification from Unread to Read.

## Needs Confirmation
- None
