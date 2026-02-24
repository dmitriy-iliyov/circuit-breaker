package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedTimeWindowErrorCountStrategy implements CloseObserveStrategy {

    private final long ttlMillis;
    private long observeEndMillis;
    private final long threshold;
    private long exceptionsCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorCountStrategy(Duration ttl, long threshold) {
        this.ttlMillis = ttl.toMillis();
        this.threshold = threshold;
        this.observeEndMillis = System.currentTimeMillis() + ttlMillis;
        this.exceptionsCount = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            long currentMillis = System.currentTimeMillis();
            if (observeEndMillis < currentMillis) {
                observeEndMillis = currentMillis + ttlMillis;
                exceptionsCount = 0;
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
                observeEndMillis = currentMillis + ttlMillis;
                exceptionsCount = 1;
                shouldTrip = false;
            }
            exceptionsCount++;
            shouldTrip = exceptionsCount >= threshold;
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
            exceptionsCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
