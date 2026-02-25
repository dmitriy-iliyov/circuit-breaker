package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitingRequestFixedTimeWindowErrorRateStrategyUnitTests {

    private WaitingRequestFixedTimeWindowErrorRateStrategy strategy;
    private static final int MIN_REQUESTS = 5;
    private static final double THRESHOLD = 0.5;
    private static final Duration WINDOW_TIME = Duration.ofMillis(1000);

    @BeforeEach
    public void refreshStrategy() {
        strategy = new WaitingRequestFixedTimeWindowErrorRateStrategy(WINDOW_TIME, THRESHOLD, MIN_REQUESTS);
    }

    @Test
    @DisplayName("UT: should NOT trip when request count is below the minimum, even if error rate is high")
    public void shouldNotTrip_whenRequestCountBelowMinimum() {
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
    @DisplayName("UT: should NOT trip when request count is sufficient but error rate is below threshold")
    public void shouldNotTrip_whenThresholdNotReached() {
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
    @DisplayName("UT: should trip when request count is sufficient and error rate reaches threshold")
    public void shouldTrip_whenThresholdReached() {
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

//    @Test
//    @DisplayName("UT: should reset state after time window expires")
//    public void shouldResetState_afterTimeWindowExpires() throws InterruptedException {
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
//        Thread.sleep(WINDOW_TIME.toMillis() + 50);
//
//        strategy.onRequest();
//
//        assertThat(strategy.shouldTrip()).isFalse();
//    }
//
//    @Test
//    @DisplayName("UT: reset() method should clear state and shouldTrip should be false")
//    public void resetMethod_shouldClearState() {
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
//        assertThat(strategy.shouldTrip()).isFalse();
//
//        strategy.onException();
//        strategy.onException();
//        assertThat(strategy.shouldTrip()).isFalse();
//    }
}
