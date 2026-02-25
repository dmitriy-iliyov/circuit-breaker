package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that trips the circuit breaker when the number of exceptions
 * exceeds a threshold within a fixed time window.
 */
public class FixedTimeWindowErrorCountStrategy implements CloseObserveStrategy {

    private final long observeTimeMillis;
    private long observeEndMillis;
    private final long exceptionCountThreshold;
    private long exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorCountStrategy(Duration observeTime, long exceptionCountThreshold) {
        Objects.requireNonNull(observeTime, "cannot be null");
        this.observeTimeMillis = observeTime.toMillis();
        if (exceptionCountThreshold < 0) {
            throw new IllegalArgumentException("exceptionCountThreshold cannot be negative");
        }
        this.exceptionCountThreshold = exceptionCountThreshold;
        this.observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
        this.exceptionCount = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (observeEndMillis < currentMillis) {
                observeEndMillis = currentMillis + observeTimeMillis;
                exceptionCount = 0;
                shouldTrip = false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onException() {
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (observeEndMillis < currentMillis) {
                observeEndMillis = currentMillis + observeTimeMillis;
                exceptionCount = 0;
                shouldTrip = false;
            }
            exceptionCount++;
            shouldTrip = exceptionCount >= exceptionCountThreshold;
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
            observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
