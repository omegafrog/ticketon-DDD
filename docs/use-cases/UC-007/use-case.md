<!-- harness-reverse-engineered:v1 -->
# UC-007 Join Queue
Actor: User. Main: join event queue and poll position/current entry status. Failure: event not OPEN or duplicate conflicting queue registration. Evidence: broker/ui/PollingWaitingQueueController.java; broker/app/PollingWaitingQueueService.java.
