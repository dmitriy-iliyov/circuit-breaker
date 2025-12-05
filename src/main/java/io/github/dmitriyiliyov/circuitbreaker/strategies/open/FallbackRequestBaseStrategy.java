package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FallbackRequestBaseStrategy implements OpenObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final int requestCount;
    private final CircuitBreakerFallback fallback;
    private int currentRequestCount;
    private final Lock lock = new ReentrantLock();

    public FallbackRequestBaseStrategy(int requestCount, CircuitBreakerFallback fallback) {
        this.requestCount = requestCount;
        this.fallback = fallback;
        reset();
    }

    @Override
    public void observe(Runnable process, Runnable callback) {
        handleRequest(callback);
        throw exception;
    }

    @Override
    public <T> T observe(Supplier<T> process, Runnable callback) {
        handleRequest(callback);
        return fallback.get();
    }

    private void handleRequest(Runnable callback) {
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
    }

    @Override
    public void reset() {
        currentRequestCount = 0;
    }
}
