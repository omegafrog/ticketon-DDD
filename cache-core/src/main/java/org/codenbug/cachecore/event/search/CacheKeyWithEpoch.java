package org.codenbug.cachecore.event.search;

public abstract class CacheKeyWithEpoch {

    private final long epoch;

    protected CacheKeyWithEpoch(long epoch) {
        this.epoch = epoch;
    }

    public long getEpoch() {
        return epoch;
    }

    public boolean isSameEpoch(long otherEpoch) {
        return epoch == otherEpoch;
    }
}
