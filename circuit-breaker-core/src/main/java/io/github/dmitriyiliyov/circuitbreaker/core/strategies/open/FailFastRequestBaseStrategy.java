package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FailFastRequestBaseStrategy implements ObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final int requestCount;
    private final Runnable callback;
    private int currentRequestCount;
    private final Lock lock = new ReentrantLock();

    public FailFastRequestBaseStrategy(int requestCount, Runnable callback) {
        this.requestCount = requestCount;
        this.callback = callback;
        reset();
    }

    @Override
    public void observe(Runnable process) {
        throw handleRequest();
    }

    @Override
    public <T> T observe(Supplier<T> process) {
        throw handleRequest();
    }

    private CircuitBreakerOpenException handleRequest() {
        lock.lock();
        try {
            currentRequestCount++;
            if (currentRequestCount > requestCount) {
                callback.run();
                reset();
            }
        } finally {
            lock.unlock();
        }
        return exception;
    }

    @Override
    public void reset() {
        currentRequestCount = 0;
    }
}
