package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeCountBasedHalfOpenStrategy implements HalfOpenStateStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private final AtomicReference<HalfOpenTransition> transition;
    private final AtomicInteger requestCount;
    private final AtomicInteger exceptionCount;

    public LockFreeCountBasedHalfOpenStrategy(int windowSize, int exceptionCountThreshold) {
        this.windowSize = windowSize;
        this.exceptionCountThreshold = exceptionCountThreshold;
        this.transition = new AtomicReference<>(HalfOpenTransition.NO_TRANSITION);
        this.requestCount = new AtomicInteger(0);
        this.exceptionCount = new AtomicInteger(0);
    }

    @Override
    public void onSuccess() {
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
