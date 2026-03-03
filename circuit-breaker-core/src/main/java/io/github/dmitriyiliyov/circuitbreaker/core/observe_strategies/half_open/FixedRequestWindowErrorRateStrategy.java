package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.half_open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenTransition;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link HalfOpenObserveStrategy} that uses a fixed request window to determine
 * the transition from HALF_OPEN.
 * <p>
 * If the error rate within the window exceeds a threshold, it transitions to OPEN.
 * If the window completes without exceeding the threshold, it transitions to CLOSED.
 */
public class FixedRequestWindowErrorRateStrategy implements HalfOpenObserveStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private int requestCount;
    private int exceptionCount;
    private volatile HalfOpenTransition transition;
    private Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorRateStrategy(int windowSize, double exceptionRateThreshold) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold must be >= 0");
        }
        this.exceptionCountThreshold = (int) Math.ceil(windowSize * exceptionRateThreshold);
        this.requestCount = 0;
        this.exceptionCount = 0;
        this.transition = HalfOpenTransition.NO_TRANSITION;
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            requestCount++;
            if (HalfOpenTransition.NO_TRANSITION.equals(transition) && requestCount >= windowSize) {
                transition = HalfOpenTransition.TO_CLOSE;
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
            exceptionCount++;
            if (exceptionCount >= exceptionCountThreshold) {
                transition = HalfOpenTransition.TO_OPEN;
            }
            if (HalfOpenTransition.NO_TRANSITION.equals(transition) && requestCount >= windowSize) {
                transition = HalfOpenTransition.TO_CLOSE;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public HalfOpenTransition getTransition() {
        return transition;
    }

    @Override
    public void reset() {
        lock.lock();
        try {
            requestCount = 0;
            exceptionCount = 0;
            transition = HalfOpenTransition.NO_TRANSITION;
        } finally {
            lock.unlock();
        }
    }
}
