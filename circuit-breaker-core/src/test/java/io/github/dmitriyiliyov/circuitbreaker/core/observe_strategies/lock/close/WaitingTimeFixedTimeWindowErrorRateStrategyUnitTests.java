package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitingTimeFixedTimeWindowErrorRateStrategyUnitTests {

    private WaitingTimeFixedTimeWindowErrorRateStrategy strategy;
    private static final Duration OBSERVE_START_TIME = Duration.ofMillis(100);
    private static final double THRESHOLD = 0.5;
    private static final Duration WINDOW_TIME = Duration.ofMillis(200);

    @BeforeEach
    public void refreshStrategy() {
        strategy = new WaitingTimeFixedTimeWindowErrorRateStrategy(WINDOW_TIME, THRESHOLD, OBSERVE_START_TIME);
    }

    @Test
    @DisplayName("UT: should NOT trip when time is below observe start time, even if error rate is high")
    public void shouldNotTrip_whenTimeBelowObserveStartTime() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 3; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: should NOT trip when time is sufficient but error rate is below threshold")
    public void shouldNotTrip_whenThresholdNotReached() throws InterruptedException {
        Thread.sleep(OBSERVE_START_TIME.toMillis() + 10);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: should trip when time is sufficient and error rate reaches threshold")
    public void shouldTrip_whenThresholdReached() throws InterruptedException {
        Thread.sleep(OBSERVE_START_TIME.toMillis() + 10);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 3; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(strategy.shouldTrip()).isTrue();
    }

    @Test
    @DisplayName("UT: should reset state after time window expires")
    public void shouldResetState_afterTimeWindowExpires() throws InterruptedException {
        Thread.sleep(OBSERVE_START_TIME.toMillis() + 10);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isTrue();

        Thread.sleep(WINDOW_TIME.toMillis() + 50);

        strategy.onRequest();

        assertThat(strategy.shouldTrip()).isFalse();
    }

//    @Test
//    @DisplayName("UT: reset() method should clear state and shouldTrip should be false")
//    public void resetMethod_shouldClearState() throws InterruptedException {
//        Thread.sleep(OBSERVE_START_TIME.toMillis() + 10);
//
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        for (int i = 0; i < 3; i++) {
//            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
//        }
//        for (int i = 0; i < 2; i++) {
//            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
//        }
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        assertThat(strategy.shouldTrip()).isTrue();
//
//        strategy.reset();
//
//        assertThat(strategy.shouldTrip()).isFalse();
//
//        strategy.onException();
//        strategy.onException();
//        assertThat(strategy.shouldTrip()).isFalse();
//    }
}
