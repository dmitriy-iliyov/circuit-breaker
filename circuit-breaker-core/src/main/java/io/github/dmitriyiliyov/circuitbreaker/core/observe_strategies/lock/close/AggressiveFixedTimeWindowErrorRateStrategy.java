package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An aggressive {@link CloseObserveStrategy} that trips the circuit breaker when the error rate
 * exceeds a threshold within a fixed time window.
 * <p>
 * This strategy is "aggressive" because it starts counting requests and exceptions immediately
 * without waiting for a minimum number of requests.
 */
public class AggressiveFixedTimeWindowErrorRateStrategy implements CloseObserveStrategy {

    private final long observeTimeMillis;
    private final double exceptionRateThreshold;
    private long observeEndMillis;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public AggressiveFixedTimeWindowErrorRateStrategy(Duration observeTime, double exceptionRateThreshold) {
        Objects.requireNonNull(observeTime, "cannot be null");
        this.observeTimeMillis = observeTime.toMillis();
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold cannot be negative");
        }
        this.exceptionRateThreshold = exceptionRateThreshold;
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
            observeEndMillis = System.currentTimeMillis() + observeTimeMillis;
            requestCount = 0;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
