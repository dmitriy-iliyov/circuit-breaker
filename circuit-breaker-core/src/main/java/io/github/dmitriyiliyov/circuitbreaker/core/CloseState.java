package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;

import java.util.Objects;
import java.util.function.Function;

public class CloseState implements CircuitState, ConfigurableCircuitState {

    private final CircuitBreaker circuitBreaker;
    private CircuitState nextState;
    private final CloseStateStrategy strategy;
    private final RequestTimer timer;
    private final Function<Throwable, Boolean> checker;

    CloseState(CircuitBreaker circuitBreaker, CloseStateStrategy strategy, RequestTimer timer) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.timer = Objects.requireNonNull(timer, "timer cannot be null");
        this.checker = circuitBreaker.getChecker();
    }

    public CloseState(CircuitBreaker circuitBreaker, CircuitState nextState, CloseStateStrategy strategy, RequestTimer timer) {
        this(circuitBreaker, strategy, timer);
        this.nextState = Objects.requireNonNull(nextState, "nextState cannot be null");
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
