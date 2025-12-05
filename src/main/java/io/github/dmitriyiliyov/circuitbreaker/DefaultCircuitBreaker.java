package io.github.dmitriyiliyov.circuitbreaker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class DefaultCircuitBreaker implements CircuitBreaker {

    private final Set<Class<?>> observableExceptions;
    private CircuitState state;
    private final Lock lock = new ReentrantLock();

    public DefaultCircuitBreaker(Set<Class<?>> observableExceptions) {
        this.observableExceptions = Collections.synchronizedSet(observableExceptions);
    }

    public DefaultCircuitBreaker(Set<Class<?>> observableExceptions, CircuitState state) {
        this.observableExceptions = Collections.synchronizedSet(observableExceptions);
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
        lock.lock();
        try {
            state.reset();
            this.state = state;
        } finally {
            lock.unlock();
        }
    }

    public CircuitState getState() {
        return state;
    }
}
