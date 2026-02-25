package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that trips the circuit breaker when the error rate
 * exceeds a threshold within a fixed number of requests.
 */
public class FixedRequestWindowErrorRateStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorRateStrategy(int windowSize, double exceptionRateThreshold) {
        if (windowSize < 0) {
            throw new IllegalArgumentException("windowSize cannot be negative");
        }
        this.windowSize = windowSize;
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold cannot be negative");
        }
        this.exceptionCountThreshold = (int) Math.ceil(exceptionRateThreshold * windowSize);
        this.requestCount = 0;
        this.exceptionCount = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
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
