package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open.HalfOpenObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open.HalfOpenTransition;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class HalfOpenState implements CircuitState {

    private final CircuitBreaker circuitBreaker;
    private CircuitState openState;
    private CircuitState closeState;
    private final HalfOpenObserveStrategy strategy;
    private final Function<Throwable, Boolean> checker;

    public HalfOpenState(CircuitBreaker circuitBreaker, HalfOpenObserveStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.checker = (throwable) -> circuitBreaker.getObservableExceptions()
                .stream()
                .anyMatch(e -> e.isInstance(throwable));
    }

    public HalfOpenState(CircuitBreaker circuitBreaker, CircuitState openState, CircuitState closeState, HalfOpenObserveStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.openState = Objects.requireNonNull(openState, "openState cannot be null");
        this.closeState = Objects.requireNonNull(closeState, "closeState cannot be null");
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
        if (HalfOpenTransition.NO_TRANSITION.equals(strategy.getTransition())) {
            return;
        }
        if (HalfOpenTransition.TO_OPEN.equals(strategy.getTransition())) {
            if (circuitBreaker.trySetState(this, openState)) {
                strategy.reset();
            }
        }
        if (HalfOpenTransition.TO_CLOSE.equals(strategy.getTransition())) {
            if (circuitBreaker.trySetState(this, closeState)) {
                strategy.reset();
            }
        }
    }

    public void setCloseState(CircuitState closeState) {
        this.closeState = Objects.requireNonNull(closeState, "closeState cannot be null");
    }

    public void setOpenState(CircuitState openState) {
        this.openState = Objects.requireNonNull(openState, "openState cannot be null");
    }
}
