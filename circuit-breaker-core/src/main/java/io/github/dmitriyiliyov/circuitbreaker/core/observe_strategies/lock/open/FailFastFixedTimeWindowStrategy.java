package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link OpenObserveStrategy} that transitions to HALF_OPEN after a fixed time window.
 */
public class FailFastFixedTimeWindowStrategy implements OpenObserveStrategy {

    private final long ttlMillis;
    private long observeEndMillis;
    private boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FailFastFixedTimeWindowStrategy(Duration ttl) {
        this.ttlMillis = ttl.toMillis();
        this.shouldTrip = false;
        this.observeEndMillis = System.currentTimeMillis() + ttlMillis;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (currentMillis > observeEndMillis) {
                observeEndMillis = currentMillis + ttlMillis;
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
            shouldTrip = false;
            observeEndMillis = System.currentTimeMillis() + ttlMillis;
        } finally {
            lock.unlock();
        }
    }
}
