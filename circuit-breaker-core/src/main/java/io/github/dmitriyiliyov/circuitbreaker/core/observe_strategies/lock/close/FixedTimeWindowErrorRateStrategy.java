package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedTimeWindowErrorRateStrategy implements CloseObserveStrategy {

    private final long ttlMillis;
    private final double threshold;
    private long observeEndMillis;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorRateStrategy(Duration ttl, double threshold) {
        this.ttlMillis = ttl.toMillis();
        this.threshold = threshold;
        this.observeEndMillis = System.currentTimeMillis() + ttlMillis;
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
                observeEndMillis = currentMillis + ttlMillis;
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
                observeEndMillis = currentMillis + ttlMillis;
                requestCount = 1;
                exceptionCount = 1;
                shouldTrip = false;
            }
            exceptionCount++;
            shouldTrip = (double) exceptionCount / requestCount >= threshold;
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
            observeEndMillis = System.currentTimeMillis() + ttlMillis;
            requestCount = 0;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
