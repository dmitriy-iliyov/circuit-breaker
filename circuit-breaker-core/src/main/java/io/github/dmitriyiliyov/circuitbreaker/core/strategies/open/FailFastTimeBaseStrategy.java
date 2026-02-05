package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FailFastTimeBaseStrategy implements ObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final Duration ttl;
    private final Runnable callback;
    private Instant observeEnd;
    private final Lock lock = new ReentrantLock();

    public FailFastTimeBaseStrategy(Duration ttl, Runnable callback) {
        this.ttl = ttl;
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
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
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
        observeEnd = Instant.now().plus(ttl);
    }
}
