package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedRequestWindowErrorCountStrategyUnitTests {

    private FixedRequestWindowErrorCountStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FixedRequestWindowErrorCountStrategy(10, 2);
    }

    @Test
    @DisplayName("UT: all requests without exceptions should result in transition TO_CLOSE")
    public void allRequestWithoutExceptions_shouldTransitionToClose() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);
    }

    @Test
    @DisplayName("UT: exception count threshold not reached should result in transition TO_CLOSE")
    public void exceptionCountThresholdNotReached_shouldTransitionToClose() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 1; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);
    }

    @Test
    @DisplayName("UT: exception count threshold reached should result in transition TO_OPEN")
    public void exceptionCountThresholdReached_shouldTransitionToOpen() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onException()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_OPEN);
    }

    @Test
    @DisplayName("UT: not enough requests should result in NO_TRANSITION")
    public void notEnoughRequests_shouldResultInNoTransition() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.NO_TRANSITION);
    }

    @Test
    @DisplayName("UT: reset should clear state and transition should be NO_TRANSITION")
    public void reset_shouldClearStateAndTransitionShouldBeNoTransition() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> strategy.onRequest()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);

        strategy.reset();

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.NO_TRANSITION);
        strategy.onRequest();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.NO_TRANSITION);
    }
}
