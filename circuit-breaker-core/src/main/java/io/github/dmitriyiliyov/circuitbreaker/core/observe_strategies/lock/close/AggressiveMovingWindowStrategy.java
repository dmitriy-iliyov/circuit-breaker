package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AggressiveMovingWindowStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final double threshold;
    private int [] window;
    private int index;
    private int windowSum;
    private int currentWindowSize;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public AggressiveMovingWindowStrategy(int windowSize, double threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        this.window = new int[windowSize];
        this.index = 0;
        this.windowSum = 0;
        this.currentWindowSize = 0;
        this.shouldTrip = false;
    }

    @Override
    public void onRequest() {
        updateWindow(0);
    }

    @Override
    public void onException() {
        updateWindow(1);
    }

    private void updateWindow(int value) {
        lock.lock();
        try {
            if (currentWindowSize < windowSize) {
                currentWindowSize++;
            }
            if (currentWindowSize == windowSize) {
                windowSum -= window[index];
            }
            windowSum += value;
            window[index] = value;
            index = (index + 1) % windowSize;
            shouldTrip = (double) windowSum / currentWindowSize >= threshold;
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
            window = new int[windowSize];
            index = 0;
            windowSum = 0;
            currentWindowSize = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
