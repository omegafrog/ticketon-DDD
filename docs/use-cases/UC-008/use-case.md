<!-- harness-reverse-engineered:v1 -->
# UC-008 Leave Queue
Actor: User. Main: remove own event queue registration. Failure: no registration. Evidence: broker/ui/PollingWaitingQueueController.java; broker/infra/WaitingQueueRedisRepository.java.
