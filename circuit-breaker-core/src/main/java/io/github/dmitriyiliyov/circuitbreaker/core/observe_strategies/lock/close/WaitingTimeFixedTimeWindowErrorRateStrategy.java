package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that trips the circuit breaker when the error rate
 * exceeds a threshold within a fixed time window, but only after a certain
 * observation start time has passed.
 */
public class WaitingTimeFixedTimeWindowErrorRateStrategy implements CloseObserveStrategy {

    private final long observeTimeMillis;
    private final double exceptionRateThreshold;
    private final long observeStartMillis;
    private long observeEndMillis;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public WaitingTimeFixedTimeWindowErrorRateStrategy(Duration observeTime, double exceptionRateThreshold, Duration observeStartTime) {
        Objects.requireNonNull(observeTime, "cannot be null");
        this.observeTimeMillis = observeTime.toMillis();
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold cannot be negative");
        }
        this.exceptionRateThreshold = exceptionRateThreshold;
        this.observeStartMillis = System.currentTimeMillis() + observeStartTime.toMillis();
        this.observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
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
            if (currentMillis > observeEndMillis) {
                observeEndMillis = currentMillis + observeTimeMillis;
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
            if (currentMillis > observeEndMillis) {
                observeEndMillis = currentMillis + observeTimeMillis;
                requestCount = 1;
                exceptionCount = 0;
                shouldTrip = false;
            }
            exceptionCount++;
            if (currentMillis >= observeStartMillis) {
                shouldTrip = (double) exceptionCount / requestCount >= exceptionRateThreshold;
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
            observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
            requestCount = 0;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
