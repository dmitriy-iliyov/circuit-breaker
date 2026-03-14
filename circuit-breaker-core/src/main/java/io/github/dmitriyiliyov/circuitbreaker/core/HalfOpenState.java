package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenTransition;

import java.util.Objects;
import java.util.function.Function;

public class HalfOpenState implements CircuitState, ConfigurableHalfOpenState {

    private final CircuitBreaker circuitBreaker;
    private CircuitState openState;
    private CircuitState closeState;
    private final HalfOpenStateStrategy strategy;
    private final Function<Throwable, Boolean> checker;

    HalfOpenState(CircuitBreaker circuitBreaker, HalfOpenStateStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.checker = circuitBreaker.getChecker();
    }

    public HalfOpenState(CircuitBreaker circuitBreaker, CircuitState openState, CircuitState closeState, HalfOpenStateStrategy strategy) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.openState = Objects.requireNonNull(openState, "openState cannot be null");
        this.closeState = Objects.requireNonNull(closeState, "closeState cannot be null");
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
        if (HalfOpenTransition.NO_TRANSITION.equals(strategy.getTransition())) {
            return;
        }
        if (HalfOpenTransition.TO_OPEN.equals(strategy.getTransition())) {
            if (circuitBreaker.trySetState(this, openState)) {
                strategy.reset();
            }
        } else if (HalfOpenTransition.TO_CLOSE.equals(strategy.getTransition())) {
            if (circuitBreaker.trySetState(this, closeState)) {
                strategy.reset();
            }
        }
    }

    @Override
    public void setCloseState(CircuitState closeState) {
        if (this.closeState == null) {
            this.closeState = Objects.requireNonNull(closeState, "closeState cannot be null");
        } else {
            throw new IllegalStateException("cannot modify state with this method");
        }
    }

    @Override
    public void setOpenState(CircuitState openState) {
        if (this.openState == null) {
            this.openState = Objects.requireNonNull(openState, "openState cannot be null");
        } else {
            throw new IllegalStateException("cannot modify state with this method");
        }
    }

    CircuitState getOpenState() {
        return openState;
    }

    CircuitState getCloseState() {
        return closeState;
    }
}
