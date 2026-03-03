package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link OpenObserveStrategy} that transitions to HALF_OPEN after a fixed number of requests.
 */
public class FailFastFixedRequestWindowStrategy implements OpenObserveStrategy {

    private final int windowSize;
    private final AtomicInteger requestCount;
    private final Lock lock = new ReentrantLock();

    public FailFastFixedRequestWindowStrategy(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        this.requestCount = new AtomicInteger(0);
    }

    @Override
    public void onRequest() {
        requestCount.incrementAndGet();
    }

    @Override
    public boolean shouldTrip() {
        return requestCount.get() >= windowSize;
    }

    @Override
    public void reset() {
        lock.lock();
        try {
            requestCount.set(0);
        } finally {
            lock.unlock();
        }
    }
}
