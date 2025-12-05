package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FallbackTimeBaseStrategy implements OpenObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final Duration ttl;
    private final CircuitBreakerFallback fallback;
    private Instant observeEnd;
    private final Lock lock = new ReentrantLock();

    public FallbackTimeBaseStrategy(Duration ttl, CircuitBreakerFallback fallback) {
        this.ttl = ttl;
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
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
                callback.run();
                reset();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        observeEnd = Instant.now().plus(ttl);
    }
}
