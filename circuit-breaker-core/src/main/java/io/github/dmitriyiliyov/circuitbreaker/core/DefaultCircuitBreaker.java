package io.github.dmitriyiliyov.circuitbreaker.core;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class DefaultCircuitBreaker implements CircuitBreaker {

    private final Set<Class<? extends Throwable>> observableExceptions;
    private volatile CircuitState state;
    private final Lock lock = new ReentrantLock();

    public DefaultCircuitBreaker(Set<Class<? extends Throwable>> observableExceptions) {
        this.observableExceptions = Set.copyOf(observableExceptions);
    }

    public DefaultCircuitBreaker(Set<Class<? extends Throwable>> observableExceptions, CircuitState state) {
        this.observableExceptions = Set.copyOf(observableExceptions);
        this.state = state;
    }

    @Override
    public void execute(Runnable process) {
        state.execute(process);
    }

    @Override
    public <T> T execute(Supplier<T> process) {
        return state.execute(process);
    }

    @Override
    public Set<Class<? extends Throwable>> getObservableExceptions() {
        return observableExceptions;
    }

    @Override
    public boolean trySetState(CircuitState previousState, CircuitState nextState) {
        lock.lock();
        try {
            if (state.equals(previousState)) {
                this.state = nextState;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void setState(CircuitState state) {
        this.state = state;
    }

    @Override
    public CircuitState getState() {
        return state;
    }
}
