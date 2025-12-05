package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FailFastRequestBaseStrategy implements OpenObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final int requestCount;
    private int currentRequestCount;
    private final Lock lock = new ReentrantLock();

    public FailFastRequestBaseStrategy(int requestCount) {
        this.requestCount = requestCount;
        reset();
    }

    @Override
    public void observe(Runnable process, Runnable callback) {
        throw handleRequest(callback);
    }

    @Override
    public <T> T observe(Supplier<T> process, Runnable callback) {
        throw handleRequest(callback);
    }

    private CircuitBreakerOpenException handleRequest(Runnable callback) {
        lock.lock();
        try {
            currentRequestCount++;
            if (currentRequestCount >= requestCount) {
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
