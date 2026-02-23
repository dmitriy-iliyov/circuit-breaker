package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedRequestWindowErrorCountStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final long threshold;
    private int requestCount;
    private int exceptionsCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorCountStrategy(int windowSize, long threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        this.requestCount = 0;
        this.exceptionsCount = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            requestCount++;
            if (requestCount > windowSize) {
                requestCount = 1;
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
            requestCount++;
            if (requestCount > windowSize) {
                requestCount = 1;
                exceptionsCount = 0;
                shouldTrip = false;
            }
            exceptionsCount++;
            if (exceptionsCount >= threshold) {
                shouldTrip = true;
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
            requestCount = 0;
            exceptionsCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
