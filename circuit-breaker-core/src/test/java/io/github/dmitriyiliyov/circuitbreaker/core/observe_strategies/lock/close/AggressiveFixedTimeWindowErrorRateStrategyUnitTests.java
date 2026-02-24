package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class AggressiveFixedTimeWindowErrorRateStrategyUnitTests {

    private AggressiveFixedTimeWindowErrorRateStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new AggressiveFixedTimeWindowErrorRateStrategy(Duration.ofMillis(100), 0.2);
    }

    @Test
    @DisplayName("UT: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: exception frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 1; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: exception frequency threshold reached should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_shouldTripShouldBeTrue() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);
    }

    @Test
    @DisplayName("UT: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse() throws InterruptedException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);

        Thread.sleep(150);

        futures.clear();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: when time window expired should result in shouldTrip being false")
    public void timeWindowExpired_shouldTripShouldBeFalse() throws InterruptedException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        for (int i = 0; i < 7; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(strategy.shouldTrip()).isEqualTo(true);

        Thread.sleep(150);

        futures.clear();

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(true);
    }

    @Test
    @DisplayName("UT: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(false);

        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(true);
    }
}
