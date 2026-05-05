package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class LockFreeSlidingWindowCloseStrategy implements CloseStateStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private final long initialDelayMillis;
    private final AtomicIntegerArray window;
    private final AtomicInteger index;
    private final AtomicBoolean shouldTrip;

    public LockFreeSlidingWindowCloseStrategy(int windowSize, int exceptionCountThreshold, Duration initialDelay) {
        this.windowSize = windowSize;
        this.exceptionCountThreshold = exceptionCountThreshold;
        this.initialDelayMillis = System.currentTimeMillis() + initialDelay.toMillis();
        this.window = new AtomicIntegerArray(windowSize);
        this.index = new AtomicInteger(0);
        this.shouldTrip = new AtomicBoolean(false);
    }

    @Override
    public void onSuccess() {
        if (System.currentTimeMillis() < initialDelayMillis) {
            return;
        }
        recordEvent(0);
    }

    @Override
    public void onException() {
        if (System.currentTimeMillis() < initialDelayMillis) {
            return;
        }
        recordEvent(1);
    }

    @Override
    public boolean shouldTrip() {
        return shouldTrip.get();
    }

    private void recordEvent(int value) {
        int currentIndex = index.getAndIncrement() % windowSize;
        window.set(currentIndex, value);
        int exceptionCount = calculateExceptionCount();
        shouldTrip.set(exceptionCount >= exceptionCountThreshold);
    }

    private int calculateExceptionCount() {
        int count = 0;
        for (int i = 0; i < windowSize; i++) {
            count += window.get(i);
        }
        return count;
    }

    @Override
    public void reset() {
        for (int i = 0; i < windowSize; i++) {
            window.set(i, 0);
        }
        index.set(0);
        shouldTrip.set(false);
    }
}
