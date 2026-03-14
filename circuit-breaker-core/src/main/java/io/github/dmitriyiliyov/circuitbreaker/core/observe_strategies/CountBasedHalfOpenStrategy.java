package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link HalfOpenStateStrategy} that uses a fixed request window to determine
 * the transition from HALF_OPEN.
 * <p>
 * If the number of exceptions within the window exceeds a threshold, it transitions to OPEN.
 * If the window completes without exceeding the threshold, it transitions to CLOSED.
 */
public class CountBasedHalfOpenStrategy implements HalfOpenStateStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private int requestCount;
    private int exceptionCount;
    private volatile HalfOpenTransition transition;
    private final Lock lock = new ReentrantLock();

    public CountBasedHalfOpenStrategy(int windowSize, int exceptionCountThreshold) {
        this.windowSize = windowSize;
        this.exceptionCountThreshold = exceptionCountThreshold;
        this.requestCount = 0;
        this.exceptionCount = 0;
        this.transition = HalfOpenTransition.NO_TRANSITION;
    }

    @Override
    public void onSuccess() {
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
            if (HalfOpenTransition.NO_TRANSITION.equals(transition) && exceptionCount >= exceptionCountThreshold) {
                transition = HalfOpenTransition.TO_OPEN;
                return;
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
