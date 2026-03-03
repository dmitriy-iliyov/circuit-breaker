package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class CloseState implements CircuitState {

    private final CircuitBreaker circuitBreaker;
    private final CircuitState nextState;
    private final CloseObserveStrategy strategy;
    private final Function<Throwable, Boolean> checker;

    public CloseState(CircuitBreaker circuitBreaker, CircuitState nextState, CloseObserveStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.nextState = Objects.requireNonNull(nextState, "nextState cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.checker = (throwable) -> circuitBreaker.getObservableExceptions()
                .stream()
                .anyMatch(e -> e.isInstance(throwable));
    }

    @Override
    public void execute(Runnable process) {
        try {
            process.run();
            strategy.onRequest();
        } catch (Throwable throwable) {
            if (checker.apply(throwable)) {
                strategy.onException();
            } else {
                strategy.onRequest();
            }
            throw throwable;
        } finally {
            handleTrip();
        }
    }

    @Override
    public <T> T execute(Supplier<T> process) {
        try {
            T response = process.get();
            strategy.onRequest();
            return response;
        } catch (Throwable throwable) {
            if (checker.apply(throwable)) {
                strategy.onException();
            } else {
                strategy.onRequest();
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
}
