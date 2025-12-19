package org.codenbug.event.global;

import lombok.extern.slf4j.Slf4j;
import org.codenbug.event.application.cache.CacheRefresher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheRefreshScheduler {

    private final CacheRefresher cacheRefresher;


    public CacheRefreshScheduler(CacheRefresher cacheRefresher) {
        this.cacheRefresher = cacheRefresher;
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void refresh() {
        log.debug("Cache refreshing...");
        cacheRefresher.refreshAllCaches();
    }
}
