package org.codenbug.cachecore.event.search;

public interface CacheKeyVersionManager {

    long getVersion(String regionKey);

    long bumpVersion(String regionKey);

}
