package org.codenbug.cache;

import java.util.concurrent.atomic.AtomicLong;
import org.codenbug.cachecore.event.search.CacheKeyVersionManager;

public class CacheKeyVersionManagerImpl implements CacheKeyVersionManager {

    private static AtomicLong version = new AtomicLong(0);

    @Override
    public long getVersion() {
        return version.get();
    }

    @Override
    public long bumpVersion() {
        return version.incrementAndGet();
    }
}
