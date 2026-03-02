package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that trips the circuit breaker when the number of exceptions
 * exceeds a threshold within a fixed time window.
 */
public class FixedTimeWindowErrorCountStrategy implements CloseObserveStrategy {

    private final long observeMillis;
    private final long observeStartMillis;
    private long observeEndMillis;
    private final long exceptionCountThreshold;
    private long exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorCountStrategy(Duration observeTime, int exceptionCountThreshold, Duration waitBeforeStartTime) {
        Objects.requireNonNull(observeTime, "observeTime cannot be null");
        this.observeMillis = observeTime.toMillis();
        if (exceptionCountThreshold < 0) {
            throw new IllegalArgumentException("exceptionCountThreshold must be >= 0");
        }
        this.exceptionCountThreshold = exceptionCountThreshold;
        Objects.requireNonNull(waitBeforeStartTime, "waitBeforeStartTime cannot be null");
        this.observeStartMillis = System.currentTimeMillis() + waitBeforeStartTime.toMillis();
        this.observeEndMillis = System.currentTimeMillis() + observeMillis;
        this.exceptionCount = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        if (System.currentTimeMillis() < observeStartMillis) {
            return;
        }
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (observeEndMillis < currentMillis) {
                observeEndMillis = currentMillis + observeMillis;
                exceptionCount = 0;
                shouldTrip = false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onException() {
        if (System.currentTimeMillis() < observeStartMillis) {
            return;
        }
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (observeEndMillis < currentMillis) {
                observeEndMillis = currentMillis + observeMillis;
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
            observeEndMillis = System.currentTimeMillis() + observeMillis;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
