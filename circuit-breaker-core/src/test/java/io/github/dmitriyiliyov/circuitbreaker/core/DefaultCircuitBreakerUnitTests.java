package io.github.dmitriyiliyov.circuitbreaker.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultCircuitBreakerUnitTests {

    private DefaultCircuitBreaker circuitBreaker;
    private CircuitState initialState;
    private CircuitState nextState;
    private CircuitState otherState;

    @BeforeEach
    public void setUp() {
        initialState = mock(CircuitState.class);
        nextState = mock(CircuitState.class);
        otherState = mock(CircuitState.class);
        circuitBreaker = new DefaultCircuitBreaker(Collections.emptySet(), Collections.emptySet(), initialState);
    }

    @Test
    @DisplayName("UT: trySetState should return true and update state when current state matches previousState")
    public void trySetState_shouldReturnTrueAndUpdateState_whenCurrentStateMatches() {
        boolean result = circuitBreaker.trySetState(initialState, nextState);

        assertThat(result).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(nextState);
    }

    @Test
    @DisplayName("UT: trySetState should return false and not update state when current state does not match previousState")
    public void trySetState_shouldReturnFalseAndNotUpdateState_whenCurrentStateDoesNotMatch() {
        circuitBreaker.setState(otherState);

        boolean result = circuitBreaker.trySetState(initialState, nextState);

        assertThat(result).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo(otherState);
    }

    @Test
    @DisplayName("UT: trySetState should be thread-safe")
    public void trySetState_shouldBeThreadSafe() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean successFlag = new AtomicBoolean(false);
        int[] successCount = {0};

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    if (circuitBreaker.trySetState(initialState, nextState)) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                        successFlag.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        assertThat(successCount[0]).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(nextState);
    }
}
