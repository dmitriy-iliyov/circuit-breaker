package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FailFastTimeBaseStrategy implements OpenObserveStrategy {

    private final CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
            "Circuit breaker is open, request cannot be processed"
    );
    private final Duration ttl;
    private Instant observeEnd;
    private final Lock lock = new ReentrantLock();

    public FailFastTimeBaseStrategy(Duration ttl) {
        this.ttl = ttl;
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
