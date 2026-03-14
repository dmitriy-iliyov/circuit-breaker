package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;

import java.util.Objects;

public class OpenState implements CircuitState {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be executed"
    );
    private final CircuitBreaker circuitBreaker;
    private final CircuitState nextState;
    private final OpenStateStrategy strategy;

    public OpenState(CircuitBreaker circuitBreaker, CircuitState nextState, OpenStateStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.nextState = Objects.requireNonNull(nextState, "nextState cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
    }

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        strategy.onRequest();
        if (handleTrip()) {
            process.run();
            return;
        }
        throw exception;
    }

    @Override
    public <T> T execute(CheckedSupplier<T> process) throws Throwable {
        strategy.onRequest();
        if (handleTrip()) {
            return process.get();
        }
        throw exception;
    }

    private boolean handleTrip() {
        if (strategy.shouldTransition()) {
            if (circuitBreaker.trySetState(this, nextState)) {
                strategy.reset();
                return true;
            }
        }
        return false;
    }

    CircuitState getNextState() {
        return nextState;
    }
}
