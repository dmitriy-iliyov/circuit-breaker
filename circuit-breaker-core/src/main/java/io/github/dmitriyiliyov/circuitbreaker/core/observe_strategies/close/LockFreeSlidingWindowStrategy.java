package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

public class LockFreeSlidingWindowStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final int [] window;
    private int index;
    private final int exceptionCountThreshold;
    private int windowSum;

    public LockFreeSlidingWindowStrategy(int windowSize, int [] window, double exceptionRateThreshold) {
        this.windowSize = windowSize;
        this.window = window;
        this.exceptionCountThreshold = (int) Math.ceil(windowSize * exceptionRateThreshold);
    }

    @Override
    public void onRequest() {
        moveWindow(0);
    }

    @Override
    public void onException() {
        moveWindow(0);
    }

    private void moveWindow(int value) {

    }

    @Override
    public boolean shouldTrip() {
        return false;
    }

    @Override
    public void reset() {

    }
}

