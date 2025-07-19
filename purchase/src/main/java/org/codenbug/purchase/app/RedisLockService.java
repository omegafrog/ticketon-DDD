package org.codenbug.purchase.app;

import java.util.List;

public interface RedisLockService {
	List<String> getLockedSeatIdsByUserId(String userId);

	void releaseAllLocks(String userId);

	void releaseAllEntryQueueLocks(String userId);
}
