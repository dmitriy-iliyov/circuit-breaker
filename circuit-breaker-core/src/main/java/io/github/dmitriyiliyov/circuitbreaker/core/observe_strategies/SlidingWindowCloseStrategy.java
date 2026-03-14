package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import java.time.Duration;
import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link CloseStateStrategy} that uses a sliding window to calculate the error rate.
 * The window is based on the last N requests.
 */
public class SlidingWindowCloseStrategy implements CloseStateStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private final long initialDelayMillis;
    private final BitSet window;
    private int index;
    private int windowSum;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public SlidingWindowCloseStrategy(int windowSize, int exceptionCountThreshold, Duration initialDelay) {
        this.windowSize = windowSize;
        this.exceptionCountThreshold = exceptionCountThreshold;
        this.initialDelayMillis = System.currentTimeMillis() + initialDelay.toMillis();
        this.window = new BitSet(windowSize);
        this.index = 0;
        this.windowSum = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onSuccess() {
        if (System.currentTimeMillis() < initialDelayMillis) {
            return;
        }
        moveWindow(0);
    }

    @Override
    public void onException() {
        if (System.currentTimeMillis() < initialDelayMillis) {
            return;
        }
        moveWindow(1);
    }

    @Override
    public boolean shouldTrip() {
        return shouldTrip;
    }

    private void moveWindow(int value) {
        lock.lock();
        try {
            windowSum -= window.get(index) ? 1 : 0;
            windowSum += value;
            window.set(index, value == 1);
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
            window.clear();
            index = 0;
            windowSum = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
