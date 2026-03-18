package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenTransition;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;

import java.util.Objects;
import java.util.function.Function;

public class HalfOpenState implements CircuitState, ConfigurableHalfOpenState {

    private final CircuitBreaker circuitBreaker;
    private CircuitState openState;
    private CircuitState closeState;
    private final HalfOpenStateStrategy strategy;
    private final RequestTimer timer;
    private final Function<Throwable, Boolean> checker;

    HalfOpenState(CircuitBreaker circuitBreaker, HalfOpenStateStrategy strategy, RequestTimer timer) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.timer = Objects.requireNonNull(timer, "timer cannot be null");
        this.checker = circuitBreaker.getChecker();
    }

    public HalfOpenState(CircuitBreaker circuitBreaker, CircuitState openState, CircuitState closeState, HalfOpenStateStrategy strategy, RequestTimer timer) {
        this(circuitBreaker, strategy, timer);
        this.openState = Objects.requireNonNull(openState, "openState cannot be null");
        this.closeState = Objects.requireNonNull(closeState, "closeState cannot be null");
    }

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        try {
            timer.execute(process);
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
            T response = timer.execute(process);
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
