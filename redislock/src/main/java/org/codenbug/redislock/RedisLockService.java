package org.codenbug.redislock;

import java.time.Duration;
import java.util.List;

public interface RedisLockService {
	List<String> getLockedSeatIdsByUserId(String userId);

	void releaseAllLocks(String userId);

	void releaseAllEntryQueueLocks(String userId);

	boolean tryLock(String lockKey, String lockValue, Duration duration);

	boolean unlock(String lockKey, String lockValue);

	String getLockValue(String lockKey);
}
