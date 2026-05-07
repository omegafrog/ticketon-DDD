# Purchase Process

![Asynchronous purchase confirm process](purchase-process.svg)

## Flow

1. The client starts payment with `POST /api/v1/payments/init`.
2. The purchase service creates a `Purchase` with `PaymentStatus.IN_PROGRESS`.
3. The client requests confirmation with `POST /api/v1/payments/confirm`.
4. The confirm API validates ownership, order id, amount, and deadline.
5. The API writes `purchase_confirm_status=PENDING` and a `purchase_outbox` message in the same database transaction.
6. The API returns `202 Accepted` with a status URL. It does not call the PG provider in the request thread.
7. After commit, the outbox publisher sends the work message. The scheduler retries unpublished rows if the after-commit publish fails.
8. The worker records the message in `purchase_processed_message` to make duplicate deliveries idempotent.
9. The worker updates confirm status to `PROCESSING`, checks event sales version and terminal purchase state, then calls the PG API with idempotency key `confirm:{purchaseId}`.
10. If PG confirmation succeeds, the worker finalizes the purchase locally: tickets are saved, the purchase is marked `PaymentStatus.DONE`, the seat-purchased event is published, and confirm status becomes `DONE`.
11. If PG confirmation or local finalization fails, the worker records a terminal confirm status and performs compensation when the PG already approved the payment.

## Statuses

| Status type | Values | Meaning |
| --- | --- | --- |
| Purchase payment status | `IN_PROGRESS` | Payment has been prepared and may be awaiting async confirmation. |
| Purchase payment status | `DONE` | PG confirmation and local finalization both succeeded. |
| Purchase payment status | `FAILED` | Payment processing failed before a successful final purchase. |
| Purchase payment status | `EXPIRED`, `CANCELED`, `REFUNDED`, `PARTIAL_REFUNDED` | Terminal lifecycle states outside the happy-path confirm result. |
| Confirm status projection | `PENDING` | Confirm request was accepted and the outbox row was saved. |
| Confirm status projection | `PROCESSING` | Worker is validating, calling the PG provider, or finalizing the local purchase. |
| Confirm status projection | `DONE` | Async confirm completed successfully. |
| Confirm status projection | `FAILED` | PG confirm or processing failed. |
| Confirm status projection | `REJECTED` | The event sales version changed, so the payment was not confirmed locally. |
| Confirm status projection | `COMPENSATION_REQUIRED` | PG approval succeeded, local finalization failed, and automatic PG cancellation also failed. |

## Outbox Pattern

The confirm request is accepted with a short transaction that writes both the status projection and the outbox row. Message publication happens after commit or by scheduler retry, so a request can safely return `202 Accepted` before the PG API call. The worker uses the processed-message store and the `confirm:{purchaseId}` command id to avoid duplicate PG calls and duplicate local finalization.
