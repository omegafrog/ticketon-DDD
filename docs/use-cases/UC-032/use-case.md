# UC-032. Notification Sender creates a Notification for a Notification Recipient

## Actor
- Notification Sender

## Supporting Actor
- Notification Recipient

## Goal
- Create one unread Notification for a specified Notification Recipient.

## Preconditions
- The requester is authenticated as ADMIN or MANAGER.
- Notification Sender provides Recipient User ID, Notification type, title, content, and optional target URL.

## Main Flow
1. Notification Sender submits a create request for one Notification through the authenticated API.
2. System validates Notification Sender authority and required input.
3. System stores one Notification in Unread state for the specified Recipient User ID.
4. Notification becomes available in the target Notification Recipient's Notification Inbox.

## Failure Flow
- Unauthenticated request is rejected.
- Request from a USER role is rejected.
- Blank Recipient User ID or missing required input fails without storing a Notification.

## Result
- One unread Notification is persisted for the specified Notification Recipient when the request is valid.

## Observable Constraints From Requirements
- Only ADMIN or MANAGER can create the Notification.
- Required input includes Recipient User ID, Notification type, title, and content.
- Target URL is optional.
- Successful creation stores exactly one Notification in Unread state.

## Needs Confirmation
- None
