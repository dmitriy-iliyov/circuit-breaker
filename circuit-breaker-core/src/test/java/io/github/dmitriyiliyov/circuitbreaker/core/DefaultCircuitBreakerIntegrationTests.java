package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close.CloseObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close.FixedRequestWindowErrorRateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open.FixedRequestWindowErrorCountStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open.HalfOpenObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open.FailFastFixedRequestWindowStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open.OpenObserveStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCircuitBreakerIntegrationTests {

    private final CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
            Set.of(RuntimeException.class)
    );

    @BeforeEach
    public void setUpCircuitBreaker() {
        HalfOpenObserveStrategy halfOpenObserveStrategy = new FixedRequestWindowErrorCountStrategy(20, 2);
        HalfOpenState halfOpenState = new HalfOpenState(circuitBreaker, halfOpenObserveStrategy);

        CloseObserveStrategy closeObserveStrategy = new FixedRequestWindowErrorRateStrategy(20, 0.2);
        CircuitState closeState = new CloseState(circuitBreaker, halfOpenState, closeObserveStrategy);
        ((DefaultCircuitBreaker) circuitBreaker).setState(closeState);

        OpenObserveStrategy openObserveStrategy = new FailFastFixedRequestWindowStrategy(20);
        CircuitState openState = new OpenState(circuitBreaker, closeState, openObserveStrategy);

        halfOpenState.setOpenState(openState);
        halfOpenState.setCloseState(closeState);
    }

    @Test
    @DisplayName("IT state machine, should success run")
    public void stateMachine_shouldSuccessRun() {
        // close state
        List<CompletableFuture<Void>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Runnable runnable = () -> System.out.println("Success HTTP request");
            closeStateFutures.add(CompletableFuture.runAsync(() -> circuitBreaker.execute(runnable)));
        }
        for (int i = 0; i < 4; i++) {
            Runnable runnable = () -> {throw new RuntimeException("External HTTP error");};
            closeStateFutures.add(
                    CompletableFuture.runAsync(() -> circuitBreaker.execute(runnable))
                            .exceptionally(ex -> {
                                System.out.println(ex.getMessage());
                                return null;
                            })
            );
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        // half open state
        List<CompletableFuture<Void>> halfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Runnable runnable = () -> System.out.println("Success HTTP request");
            halfOpenStateFutures.add(CompletableFuture.runAsync(() -> circuitBreaker.execute(runnable)));
        }
        for (int i = 0; i < 2; i++) {
            Runnable runnable = () -> {throw new RuntimeException("External HTTP error");};
            halfOpenStateFutures.add(
                    CompletableFuture.runAsync(() -> circuitBreaker.execute(runnable))
                            .exceptionally(ex -> {
                                System.out.println(ex.getMessage());
                                return null;
                            })
            );
        }
        CompletableFuture.allOf(halfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        // open state
        List<CompletableFuture<Void>> openStateFutures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Runnable runnable = () -> System.out.println("Success HTTP request");
            openStateFutures.add(
                    CompletableFuture.runAsync(() -> circuitBreaker.execute(runnable))
                            .exceptionally(ex -> {
                                System.out.println(ex.getMessage());
                                return null;
                            })
            );
        }
        CompletableFuture.allOf(openStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }
}
