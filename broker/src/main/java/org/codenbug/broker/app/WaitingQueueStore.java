package org.codenbug.broker.app;

public interface WaitingQueueStore {
	record PollingAdaptiveContext(String eventStatus, Long entryQueueSlots, Long waitingQueueSize) {
	}

	boolean entryQueueCountExists(String eventId);

	void updateEntryQueueCount(String eventId, int slotCount);

	void initializeEntryAdmissionSlots(String eventId, int maxActiveShoppers, int remainingSeatCount);

	boolean releaseEntryAdmissionSlot(String eventId, int maxActiveShoppers);

	boolean recordWaitingUserIfAbsent(String eventId, String userId);

	long incrementWaitingQueueIdx(String eventId);

	void saveUserToWaitingQueue(String userId, String eventId, long idx);

	void saveAdditionalUserData(String userId, String eventId, long idx, String instanceId);

	Long getUserRank(String eventId, String userId);

	boolean deleteUserFromEntry(String userId);

	boolean deleteUserFromWaiting(String eventId, String userId);

	void deleteWaitingUserRecord(String eventId, String userId);

	void clearUserQueueEvent(String userId);

	boolean isUserExistInEntry(String userId);

	String getEntryToken(String userId);

	void updateWaitingLastSeen(String eventId, String userId, long epochMillis);

	void updateEntryLastSeen(String userId, long epochMillis);

	boolean setUserQueueEventIfAbsent(String userId, String eventId);

	String getUserQueueEvent(String userId);

	void refreshUserQueueEventTtl(String userId, long ttlSeconds);

	PollingAdaptiveContext getPollingAdaptiveContext(String eventId, boolean includeWaitingQueueSize);

	Long getEntryQueueSlots(String eventId);

	Long getWaitingQueueSize(String eventId);
}
