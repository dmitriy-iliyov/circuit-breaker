package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that trips the circuit breaker when the error rate
 * exceeds a threshold within a fixed number of requests.
 */
public class FixedRequestWindowErrorRateStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private final long observeStartMillis;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorRateStrategy(int windowSize, double exceptionRateThreshold, Duration waitBeforeStartTime) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold must be >= 0");
        }
        this.exceptionCountThreshold = (int) Math.ceil(exceptionRateThreshold * windowSize);
        Objects.requireNonNull(waitBeforeStartTime, "waitBeforeStartTime cannot be null");
        this.observeStartMillis = System.currentTimeMillis() + waitBeforeStartTime.toMillis();
        this.requestCount = 0;
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
            requestCount++;
            if (requestCount > windowSize) {
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
        if (System.currentTimeMillis() < observeStartMillis) {
            return;
        }
        lock.lock();
        try {
            requestCount++;
            if (requestCount > windowSize) {
                requestCount = 1;
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
            requestCount = 0;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
