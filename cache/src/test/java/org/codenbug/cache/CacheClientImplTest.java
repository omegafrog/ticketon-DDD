package org.codenbug.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheClientImplTest {

    @Test
    @DisplayName("여러 개의 스레드가 동시에 조회해도, 캐시 업데이트는 1회만 수행되어야 한다.")
    void get_singleFlightLoadsOnce() throws Exception {
        ExecutorService cacheLoaderExecutor = Executors.newFixedThreadPool(2);
        CacheImpl<String, String> cache = new CacheImpl<>(
            Caffeine.newBuilder().maximumSize(100).recordStats().build(),
            cacheLoaderExecutor
        );

        // 동시 호출 스레드 수
        int callers = 20;
        // 여러 스레드가 동일 지점에서 대기하도록 하는 배리어
        CyclicBarrier barrier = new CyclicBarrier(callers);
        ExecutorService callersExecutor = Executors.newFixedThreadPool(callers);
        // 로더가 몇번 실행되었는지 카운트
        AtomicInteger loadCount = new AtomicInteger(0);
        // 값이 0이 될 때까지 스레드가 대기함
        CountDownLatch loaderRelease = new CountDownLatch(1);

        // 첫번째 caller가 로더를 호출하고 나머지는 대기하게 됨
        Supplier<String> loader = () -> {
            loadCount.incrementAndGet();
            try {
                // 나머지 caller들이 CompletableFuture를 얻고 get() 호출로 대기할 때까지 기다리기 위해서
                // CountDownLatch로 대기
                loaderRelease.await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "value";
        };

        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < callers; i++) {
            futures.add(callersExecutor.submit(() -> {
                // 다수의 caller는 동시에 이 지점에서 시작
                barrier.await();
                return cache.get("key", loader);
            }));
        }

        // loader가 한번 실행될 때까지 대기
        waitUntil(loadCount, 1, 500);
        // latch를 down해서 loader가 종료되도록 함
        loaderRelease.countDown();

        for (Future<String> future : futures) {
            assertEquals("value", future.get(500, TimeUnit.MILLISECONDS));
        }
        assertEquals(1, loadCount.get());

        callersExecutor.shutdownNow();
        cacheLoaderExecutor.shutdownNow();
    }

    private static void waitUntil(AtomicInteger counter, int expected, long timeoutMs)
        throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            if (counter.get() >= expected) {
                return;
            }
            Thread.sleep(10);
        }
        assertEquals(expected, counter.get());
    }
}
