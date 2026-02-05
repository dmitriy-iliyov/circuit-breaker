package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FallbackRequestBaseStrategy implements ObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final int requestCount;
    private final CircuitBreakerFallback fallback;
    private final Runnable callback;
    private int currentRequestCount;
    private final Lock lock = new ReentrantLock();

    public FallbackRequestBaseStrategy(int requestCount, CircuitBreakerFallback fallback, Runnable callback) {
        this.requestCount = requestCount;
        this.fallback = fallback;
        this.callback = callback;
        reset();
    }

    @Override
    public void observe(Runnable process) {
        handleRequest();
        throw exception;
    }

    @Override
    public <T> T observe(Supplier<T> process) {
        handleRequest();
        return fallback.get();
    }

    private void handleRequest() {
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
    }

    @Override
    public void reset() {
        currentRequestCount = 0;
    }
}
