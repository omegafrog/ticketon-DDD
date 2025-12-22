package org.codenbug.cachecore.event.search;

public interface CacheKeyVersionManager {

    public long getVersion();

    public long bumpVersion();

}
