package org.codenbug.cache;

import com.github.benmanes.caffeine.cache.Cache;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.codenbug.cachecore.event.search.CacheClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheImpl<K, V> implements CacheClient<K, V> {

    private final Cache cache;

    private final ConcurrentHashMap<K, CompletableFuture<V>> loaderMap = new ConcurrentHashMap();
    private final Executor cacheLoaderExecutor;
    private static final Duration TIMEOUT = Duration.ofMillis(1000);
    private final AtomicLong singleFlightJoinCount = new AtomicLong(0);

    public CacheImpl(Cache cache, Executor cacheLoaderExecutor) {
        this.cache = cache;
        this.cacheLoaderExecutor = cacheLoaderExecutor;
    }


    @Override
    public void put(K cacheKey, V eventPage) {
        cache.put(cacheKey, eventPage);
    }

    @Override
    public V get(K key) {
        V result = (V) cache.getIfPresent(key);
        return result;
    }

    @Override
    public V get(K key, Supplier<V> loader) {
        V cached = (V) cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        // 키에 해당하는 CompletableFuture가 이미 있으면 그것을 받아서 처리를 대기함
        CompletableFuture<V> inFlight = loaderMap.get(key);
        if (inFlight != null) {
            long count = singleFlightJoinCount.incrementAndGet();
            log.debug("cache single-flight joined count: {}", count);
            return awaitResult(inFlight, key);
        }

        // 키에 해당하는 CompletableFuture가 없으면 새로 생성
        CompletableFuture<V> future = new CompletableFuture<>();
        // 키에 해당하는 value가 없을 경우 existing이 null이 되어 로더가 실행됨
        CompletableFuture<V> existing = loaderMap.putIfAbsent(key, future);
        if (existing != null) {
            long count = singleFlightJoinCount.incrementAndGet();
            log.debug("cache single-flight joined count: {}", count);
            return awaitResult(existing, key);
        }

        cacheLoaderExecutor.execute(() -> {
            try {
                V loaded = loader.get();
                if (loaded != null) {
                    put(key, loaded);
                    future.complete(loaded);
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            } finally {
                loaderMap.remove(key);
            }
        });

        return awaitResult(future, key);

    }

    @Override
    public boolean exist(K cacheKey) {
        return cache.getIfPresent(cacheKey) != null;
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void cacheStat() {
        log.debug("cache size: {}", cache.estimatedSize());
        log.debug("cache hit rate: {}", cache.stats().hitRate());
        log.debug("cache miss rate: {}", cache.stats().missRate());
    }

    private V awaitResult(CompletableFuture<V> result, K key) {
        try {
            return result.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            throw new RuntimeException("Cache load timeout key=" + key, te);
        } catch (ExecutionException ee) {
            Throwable c = ee.getCause();
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(c);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
    }
}
