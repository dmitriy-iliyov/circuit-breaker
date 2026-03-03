package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that trips the circuit breaker when the error rate
 * exceeds a threshold within a fixed time window, but only after a certain
 * observation start time has passed.
 */
public class FixedTimeWindowErrorRateStrategy implements CloseObserveStrategy {

    private final long observeMillis;
    private final double exceptionRateThreshold;
    private final long observeStartMillis;
    private long observeEndMillis;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorRateStrategy(Duration observeTime, double exceptionRateThreshold, Duration waitBeforeStartTime) {
        Objects.requireNonNull(observeTime, "observeTime cannot be null");
        this.observeMillis = observeTime.toMillis();
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold must be >= 0");
        }
        this.exceptionRateThreshold = exceptionRateThreshold;
        Objects.requireNonNull(waitBeforeStartTime, "waitBeforeStartTime cannot be null");
        this.observeStartMillis = System.currentTimeMillis() + waitBeforeStartTime.toMillis();
        this.observeEndMillis = observeStartMillis + observeMillis;
        this.requestCount = 0;
        this.exceptionCount = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            requestCount++;
            long currentMillis = System.currentTimeMillis();
            if (currentMillis < observeStartMillis) {
                return;
            }
            if (currentMillis > observeEndMillis) {
                observeEndMillis = currentMillis + observeMillis;
                requestCount = 1;
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
            requestCount++;
            long currentMillis = System.currentTimeMillis();
            if (currentMillis < observeStartMillis) {
                return;
            }
            if (currentMillis > observeEndMillis) {
                observeEndMillis = currentMillis + observeMillis;
                requestCount = 1;
                exceptionCount = 0;
                shouldTrip = false;
            }
            exceptionCount++;
            shouldTrip = (double) exceptionCount / requestCount >= exceptionRateThreshold;
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
            requestCount = 0;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
