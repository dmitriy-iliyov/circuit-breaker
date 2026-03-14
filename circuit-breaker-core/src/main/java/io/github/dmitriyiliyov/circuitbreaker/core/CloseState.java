package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;

import java.util.Objects;
import java.util.function.Function;

public class CloseState implements CircuitState, ConfigurableCircuitState {

    private final CircuitBreaker circuitBreaker;
    private CircuitState nextState;
    private final CloseStateStrategy strategy;
    private final Function<Throwable, Boolean> checker;

    CloseState(CircuitBreaker circuitBreaker, CloseStateStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.checker = circuitBreaker.getChecker();
    }

    public CloseState(CircuitBreaker circuitBreaker, CircuitState nextState, CloseStateStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.nextState = Objects.requireNonNull(nextState, "nextState cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.checker = circuitBreaker.getChecker();
    }

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        try {
            process.run();
            strategy.onSuccess();
        } catch (Throwable throwable) {
            if (checker.apply(throwable)) {
                strategy.onException();
            } else {
                strategy.onSuccess();
            }
            throw throwable;
        } finally {
            handleTrip();
        }
    }

    @Override
    public <T> T execute(CheckedSupplier<T> process) throws Throwable {
        try {
            T response = process.get();
            strategy.onSuccess();
            return response;
        } catch (Throwable throwable) {
            if (checker.apply(throwable)) {
                strategy.onException();
            } else {
                strategy.onSuccess();
            }
            throw throwable;
        } finally {
            handleTrip();
        }
    }

    private void handleTrip() {
        if (strategy.shouldTrip()) {
            if (circuitBreaker.trySetState(this, nextState)) {
                strategy.reset();
            }
        }
    }

    @Override
    public void setNextState(CircuitState nextState) {
        if (this.nextState == null) {
            this.nextState = Objects.requireNonNull(nextState, "nextState cannot be null");
        } else {
            throw new IllegalStateException("cannot modify state with this method");
        }
    }

    CircuitState getNextState() {
        return nextState;
    }
}
