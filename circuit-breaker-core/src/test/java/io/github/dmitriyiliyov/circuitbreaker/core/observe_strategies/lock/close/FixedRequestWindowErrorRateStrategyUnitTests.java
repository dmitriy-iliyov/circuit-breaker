package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedRequestWindowErrorRateStrategyUnitTests {

    private final int windowSize = 110;
    private final double threshold = 0.1;
    private int exceptionallyRequestCount = (int) (windowSize * threshold);
    private int successRequestCount = windowSize - exceptionallyRequestCount;
    private FixedRequestWindowErrorRateStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FixedRequestWindowErrorRateStrategy(windowSize, threshold);
    }

    @Test
    @DisplayName("UT: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < windowSize; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: exception frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < windowSize - 1; i++) {
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
        for (int i = 0; i < successRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < exceptionallyRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);
    }

    @Test
    @DisplayName("UT: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < windowSize; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);

        futures.clear();
        for (int i = 0; i < windowSize; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < successRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < exceptionallyRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(false);
        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: window reset on request overflow should clear state and shouldTrip should be false")
    public void windowResetOnRequestOverflow_shouldClearStateAndShouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < successRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < exceptionallyRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);

        futures.clear();
        for (int i = 0; i < successRequestCount / 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(strategy.shouldTrip()).isEqualTo(false);

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: window reset on exception overflow should clear state and shouldTrip should be false")
    public void windowResetOnExceptionOverflow_shouldClearStateAndShouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < successRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < exceptionallyRequestCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);

        strategy.onException();

        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }
}
