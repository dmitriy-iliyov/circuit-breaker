package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link OpenObserveStrategy} that transitions to HALF_OPEN after a fixed number of requests.
 */
public class FailFastFixedRequestWindowStrategy implements OpenObserveStrategy {

    private final int windowSize;
    private volatile boolean shouldTrip;
    private int requestCount;
    private final Lock lock = new ReentrantLock();

    public FailFastFixedRequestWindowStrategy(int windowSize) {
        this.windowSize = windowSize;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            requestCount++;
            if (requestCount >= windowSize) {
                shouldTrip = true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean shouldTrip() {
        return shouldTrip;
    }

    @Override
    public void reset() {
        lock.lock();
        try {
            requestCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
