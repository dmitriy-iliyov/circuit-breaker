package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link OpenObserveStrategy} that transitions to HALF_OPEN after a fixed time window.
 */
public class FailFastFixedTimeWindowStrategy implements OpenObserveStrategy {

    private final long observeTimeMillis;
    private long observeEndMillis;
    private boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FailFastFixedTimeWindowStrategy(Duration observeTime) {
        Objects.requireNonNull(observeTime, "observeTime cannot be null");
        this.observeTimeMillis = observeTime.toMillis();
        this.shouldTrip = false;
        this.observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (currentMillis > observeEndMillis) {
                observeEndMillis = currentMillis + observeTimeMillis;
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
            observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
        } finally {
            lock.unlock();
        }
    }
}
