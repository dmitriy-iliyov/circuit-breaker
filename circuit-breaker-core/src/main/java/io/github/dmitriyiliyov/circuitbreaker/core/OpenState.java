package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CircuitBreakerOpenException;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;

import java.util.Objects;
import java.util.function.Supplier;

public class OpenState implements CircuitState {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be executed"
    );
    private final CircuitBreaker circuitBreaker;
    private final CircuitState nextState;
    private final OpenObserveStrategy strategy;

    public OpenState(CircuitBreaker circuitBreaker, CircuitState nextState, OpenObserveStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.nextState = Objects.requireNonNull(nextState, "nextState cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
    }

    @Override
    public void execute(Runnable process) {
        strategy.onRequest();
        handleTrip();
        throw exception;
    }

    @Override
    public <T> T execute(Supplier<T> process) {
        strategy.onRequest();
        handleTrip();
        throw exception;
    }

    private void handleTrip() {
        if (strategy.shouldTrip()) {
            if (circuitBreaker.trySetState(this, nextState)) {
                strategy.reset();
            }
        }
    }
}
