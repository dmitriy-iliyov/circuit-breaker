package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link OpenObserveStrategy} that transitions to HALF_OPEN after a fixed time window.
 */
public class FailFastFixedTimeWindowStrategy implements OpenObserveStrategy {

    private final long observeMillis;
    private volatile long observeEndMillis;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FailFastFixedTimeWindowStrategy(Duration observeTime) {
        Objects.requireNonNull(observeTime, "observeTime cannot be null");
        this.observeMillis = observeTime.toMillis();
        this.shouldTrip = false;
        this.observeEndMillis = System.currentTimeMillis() + observeMillis;
    }

    @Override
    public void onRequest() {
        long currentMillis = System.currentTimeMillis();
        if (currentMillis > observeEndMillis) {
            lock.lock();
            try {
                if (currentMillis > observeEndMillis) {
                    shouldTrip = true;
                }
            } finally {
                lock.unlock();
            }
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
            observeEndMillis = System.currentTimeMillis() + observeMillis;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
