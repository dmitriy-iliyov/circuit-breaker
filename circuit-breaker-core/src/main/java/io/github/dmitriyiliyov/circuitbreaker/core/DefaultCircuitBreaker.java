package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.ConfigurableCircuitBreaker;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Default CircuitBreaker implementation.
 */
public class DefaultCircuitBreaker implements CircuitBreaker, ConfigurableCircuitBreaker {

    private final Set<Class<? extends Throwable>> observableExceptions;
    private final Set<Class<? extends Throwable>> ignorableExceptions;
    private AtomicReference<CircuitState> state;

    public DefaultCircuitBreaker(Set<Class<? extends Throwable>> observableExceptions,
                                 Set<Class<? extends Throwable>> ignorableExceptions) {
        this.observableExceptions = Set.copyOf(observableExceptions);
        this.ignorableExceptions = Set.copyOf(ignorableExceptions);
    }

    public DefaultCircuitBreaker(Set<Class<? extends Throwable>> observableExceptions,
                                 Set<Class<? extends Throwable>> ignorableExceptions,
                                 CircuitState state) {
        this.observableExceptions = Set.copyOf(observableExceptions);
        this.ignorableExceptions = Set.copyOf(ignorableExceptions);
        this.state = new AtomicReference<>(state);
    }

    @Override
    public void execute(Runnable process) {
        state.get().execute(process);
    }

    @Override
    public <T> T execute(Supplier<T> process) {
        return state.get().execute(process);
    }

    @Override
    public Set<Class<? extends Throwable>> getObservableExceptions() {
        return observableExceptions;
    }

    @Override
    public Set<Class<? extends Throwable>> getIgnorableExceptions() {
        return ignorableExceptions;
    }

    @Override
    public boolean trySetState(CircuitState previousState, CircuitState nextState) {
        return state.compareAndSet(previousState, nextState);
    }

    @Override
    public void setState(CircuitState state) {
        this.state = new AtomicReference<>(state);
    }

    @Override
    public CircuitState getState() {
        return state.get();
    }
}
