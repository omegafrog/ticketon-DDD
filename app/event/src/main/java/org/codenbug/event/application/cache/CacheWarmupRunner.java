package org.codenbug.event.application.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private final CacheRefresher cacheRefresher;

    @Override
    public void run(ApplicationArguments args) {
        cacheRefresher.refreshAllCaches();
    }

}
