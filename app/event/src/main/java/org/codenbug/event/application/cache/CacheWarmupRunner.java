package org.codenbug.event.application.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private final CacheRefresher cacheRefresher;

    @Override
    public void run(ApplicationArguments args) {
        log.debug("Cache warmup...");
        cacheRefresher.refreshAllCaches();
        log.debug("Cache warmup done.");
    }

}
