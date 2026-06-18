<!-- harness-reverse-engineered:v1 -->
# UC-011 Confirm Payment
Actor: User. Main: submit payment key/order/amount; system records idempotent command and processes PG confirmation asynchronously. Failure: duplicate conflict, expired purchase, PG rejection. Evidence: purchase/app/command/es/PurchaseConfirmCommandService.java; PurchaseConfirmWorker.java.
