package io.github.dmitriyiliyov.circuitbreaker;

import java.util.Set;
import java.util.function.Supplier;

public class DefaultCircuitBreaker implements CircuitBreaker {

    private final Set<Class<?>> observableExceptions;
    private CircuitState state;

    public DefaultCircuitBreaker(Set<Class<?>> observableExceptions) {
        this.observableExceptions = observableExceptions;
    }

    public DefaultCircuitBreaker(Set<Class<?>> observableExceptions, CircuitState state) {
        this.observableExceptions = observableExceptions;
        this.state = state;
    }

    @Override
    public void process(Runnable process) {
        state.process(process);
    }

    @Override
    public <T> T process(Supplier<T> process) {
        return state.process(process);
    }

    @Override
    public Set<Class<?>> getObservableExceptions() {
        return observableExceptions;
    }

    @Override
    public void setState(CircuitState state) {
        state.reset();
        this.state = state;
    }

    public CircuitState getState() {
        return state;
    }
}
