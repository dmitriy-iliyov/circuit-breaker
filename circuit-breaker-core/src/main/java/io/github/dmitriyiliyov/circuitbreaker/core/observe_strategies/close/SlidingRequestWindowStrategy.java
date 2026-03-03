package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseObserveStrategy} that uses a sliding window to calculate the error rate.
 * The window is based on the last N requests.
 */
public class SlidingWindowStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private int [] window;
    private int index;
    private int windowSum;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public SlidingWindowStrategy(int windowSize, double exceptionRateThreshold) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold must be >= 0");
        }
        this.exceptionCountThreshold = (int) Math.ceil(windowSize * exceptionRateThreshold);
        this.window = new int[windowSize];
        this.index = 0;
        this.windowSum = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        moveWindow(0);
    }

    @Override
    public void onException() {
        moveWindow(1);
    }

    @Override
    public boolean shouldTrip() {
        return shouldTrip;
    }

    private void moveWindow(int value) {
        lock.lock();
        try {
            windowSum -= window[index];
            windowSum += value;
            window[index] = value;
            index = (index + 1) % windowSize;
            shouldTrip = windowSum >= exceptionCountThreshold;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        lock.lock();
        try {
            window = new int[windowSize];
            index = 0;
            windowSum = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
