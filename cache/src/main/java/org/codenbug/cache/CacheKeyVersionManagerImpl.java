package org.codenbug.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;

public class CacheKeyVersionManagerImpl implements CacheKeyVersionManager {

    private static final String GLOBAL_REGION_KEY = "__GLOBAL__";
    private final ConcurrentMap<String, AtomicLong> versions = new ConcurrentHashMap<>();

    @Override
    public long getVersion(String regionKey) {
        return getCounter(regionKey).get();
    }

    @Override
    public long bumpVersion(String regionKey) {
        return getCounter(regionKey).incrementAndGet();
    }

    private AtomicLong getCounter(String regionKey) {
        String key = normalizeKey(regionKey);
        return versions.computeIfAbsent(key, ignored -> new AtomicLong(0));
    }

    private String normalizeKey(String regionKey) {
        if (regionKey == null || regionKey.isBlank()) {
            return GLOBAL_REGION_KEY;
        }
        return regionKey;
    }
}
