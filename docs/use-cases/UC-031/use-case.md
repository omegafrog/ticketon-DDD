# UC-031. Notification Recipient deletes their Notifications

## Actor
- Notification Recipient

## Supporting Actor
- None

## Goal
- Remove Notifications addressed to their own Recipient User ID from the Notification Inbox.

## Preconditions
- The requester is authenticated.
- The requester selects single, multiple, or all owned Notifications for deletion.

## Main Flow
1. Notification Recipient requests deletion for one Notification, a selected set of Notifications, or all Notifications in their Notification Inbox.
2. System validates that the requested deletion scope contains only Notifications addressed to the requester.
3. System deletes the owned Notifications within the approved scope.
4. System returns the deletion result.

## Failure Flow
- Unauthenticated request is rejected.
- Request that includes any Notification addressed to another Recipient User ID is rejected.
- Selected deletion ignores already missing owned Notifications and deletes the remaining existing owned Notifications.

## Result
- Requested owned Notifications are removed from the Notification Inbox according to the approved scope.

## Observable Constraints From Requirements
- Deletion is available for single, selected-set, and all-owned scopes.
- Ownership isolation applies to every requested Notification.
- Request containing another recipient's Notification is rejected.
- Missing owned Notifications in selected deletion do not block deletion of existing owned Notifications.

## Needs Confirmation
- None
