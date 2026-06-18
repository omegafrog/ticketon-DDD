<!-- harness-reverse-engineered:v1 -->
# UC-010 Create Pending Purchase
Actor: User. Main: submit event/seats, validate event and entry token, lock/reserve seats, initialize purchase. Failure: invalid token, unavailable seat, amount/event mismatch. Evidence: purchase/ui/command/PurchaseCommandController.java; purchase/app/command/es/PurchaseInitCommandService.java.
