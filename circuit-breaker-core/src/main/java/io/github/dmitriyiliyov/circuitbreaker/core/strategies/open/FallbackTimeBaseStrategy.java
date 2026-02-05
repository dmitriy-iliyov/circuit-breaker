package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FallbackTimeBaseStrategy implements ObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final Duration ttl;
    private final CircuitBreakerFallback fallback;
    private final Runnable callback;
    private Instant observeEnd;
    private final Lock lock = new ReentrantLock();

    public FallbackTimeBaseStrategy(Duration ttl, CircuitBreakerFallback fallback, Runnable callback) {
        this.ttl = ttl;
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
