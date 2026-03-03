package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.half_open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenTransition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeFixedRequestWindowErrorRateStrategy implements HalfOpenObserveStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private final AtomicReference<HalfOpenTransition> transition;
    private final AtomicInteger requestCount;
    private final AtomicInteger exceptionCount;

    public LockFreeFixedRequestWindowErrorRateStrategy(int windowSize, double exceptionRateThreshold) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold must be >= 0");
        }
        this.exceptionCountThreshold = (int) Math.ceil(windowSize * exceptionRateThreshold);
        this.transition = new AtomicReference<>(HalfOpenTransition.NO_TRANSITION);
        this.requestCount = new AtomicInteger(0);
        this.exceptionCount = new AtomicInteger(0);
    }

    @Override
    public void onRequest() {
        if (requestCount.incrementAndGet() >= windowSize) {
            transition.compareAndSet(HalfOpenTransition.NO_TRANSITION, HalfOpenTransition.TO_CLOSE);
        }
    }

    @Override
    public void onException() {
        if (exceptionCount.incrementAndGet() >= exceptionCountThreshold) {
            transition.compareAndSet(HalfOpenTransition.NO_TRANSITION, HalfOpenTransition.TO_OPEN);
            return;
        }
        if (requestCount.incrementAndGet() >= windowSize) {
            transition.compareAndSet(HalfOpenTransition.NO_TRANSITION, HalfOpenTransition.TO_CLOSE);
        }
    }

    @Override
    public HalfOpenTransition getTransition() {
        return transition.get();
    }

    @Override
    public void reset() {
        transition.set(HalfOpenTransition.NO_TRANSITION);
        requestCount.set(0);
        exceptionCount.set(0);
    }
}
