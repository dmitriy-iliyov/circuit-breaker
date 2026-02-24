package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class FailFastFixedRequestWindowStrategyUnitTests {

    private FailFastFixedRequestWindowStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FailFastFixedRequestWindowStrategy(10);
    }

    @Test
    @DisplayName("UT: requests below window size should result in shouldTrip being false")
    public void requestsBelowWindowSize_shouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }

    @Test
    @DisplayName("UT: requests equal to window size should result in shouldTrip being true")
    public void requestsEqualToWindowSize_shouldTripShouldBeTrue() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);
    }

    @Test
    @DisplayName("UT: requests above window size should result in shouldTrip being true")
    public void requestsAboveWindowSize_shouldTripShouldBeTrue() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);
    }

    @Test
    @DisplayName("UT: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.shouldTrip()).isEqualTo(true);

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(false);
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isEqualTo(false);
    }
}
