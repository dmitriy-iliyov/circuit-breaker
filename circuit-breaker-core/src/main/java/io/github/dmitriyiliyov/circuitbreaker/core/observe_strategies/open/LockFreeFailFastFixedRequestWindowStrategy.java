package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;

import java.util.concurrent.atomic.AtomicInteger;

public class LockFreeFailFastFixedRequestWindowStrategy implements OpenObserveStrategy {

    private final int windowSize;
    private final AtomicInteger requestCount;

    public LockFreeFailFastFixedRequestWindowStrategy(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        this.requestCount = new AtomicInteger(0);
    }

    @Override
    public void onRequest() {
        requestCount.incrementAndGet();
    }

    @Override
    public boolean shouldTrip() {
        return requestCount.get() >= windowSize;
    }

    @Override
    public void reset() {
        requestCount.set(0);
    }
}
