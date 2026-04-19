package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenTransition;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Half-Open state that implements a gradually increasing load in accordance with the multiplier
 */
public class GradualHalfOpenState implements CircuitState, ConfigurableHalfOpenState {

    private final CircuitBreaker circuitBreaker;
    private CircuitState openState;
    private CircuitState closeState;
    private final HalfOpenStateStrategy strategy;
    private final RequestTimer timer;
    private final Function<Throwable, Boolean> checker;
    private final AtomicInteger requestCount;
    private final AtomicInteger percentToLet;
    private final double multiplier;

    /**
     * Partly constructor only for using in {@link CircuitBreakerStateMachineInitializer}
     */
    GradualHalfOpenState(CircuitBreaker circuitBreaker, HalfOpenStateStrategy strategy, RequestTimer timer, double multiplier) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.timer = Objects.requireNonNull(timer, "timer cannot be null");
        this.checker = circuitBreaker.getChecker();
        this.requestCount = new AtomicInteger(0);
        this.percentToLet = new AtomicInteger(10);
        this.multiplier = multiplier;
    }

    public GradualHalfOpenState(CircuitBreaker circuitBreaker, CircuitState openState, CircuitState closeState,
                                HalfOpenStateStrategy strategy, RequestTimer timer, double multiplier) {
        this(circuitBreaker, strategy, timer, multiplier);
        this.openState = Objects.requireNonNull(openState, "openState cannot be null");
        this.closeState = Objects.requireNonNull(closeState, "closeState cannot be null");
    }

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        if (!shouldExecute()) {
            throw new GradualHalfOpenRefuseException();
        }
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
        if (!shouldExecute()) {
            throw new GradualHalfOpenRefuseException();
        }
        try {
            T result = timer.execute(process);
            strategy.onSuccess();
            return result;
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

    private boolean shouldExecute() {
        int currentPercent = percentToLet.get();
        if (currentPercent >= 100) {
            return true;
        }
        return requestCount.incrementAndGet() % (100 / currentPercent) == 0;
    }

    private void handleTrip() {
        HalfOpenTransition currentTransition = strategy.getTransition();
        if (HalfOpenTransition.NO_TRANSITION.equals(currentTransition)) {
            return;
        }
        if (HalfOpenTransition.TO_OPEN.equals(currentTransition)) {
            if (circuitBreaker.trySetState(this, openState)) {
                strategy.reset();
                percentToLet.set(10);
            }
        } else if (HalfOpenTransition.TO_CLOSE.equals(currentTransition)) {
            int currentPercent = percentToLet.get();
            if (currentPercent >= 100) {
                if (circuitBreaker.trySetState(this, closeState)) {
                    strategy.reset();
                    percentToLet.set(10);
                }
            } else if (percentToLet.compareAndSet(currentPercent, calculateNewPercent(currentPercent))) {
                strategy.reset();
            }
        }
    }

    private int calculateNewPercent(int currentPercent) {
        return Math.min(100, (int) (currentPercent * multiplier));
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
}

