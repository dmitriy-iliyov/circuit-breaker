package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedRequestWindowErrorCountStrategy implements HalfOpenObserveStrategy {

    private final int windowSize;
    private final long threshold;
    private int requestCount;
    private int exceptionsCount;
    private volatile HalfOpenTransition transition;
    private final Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorCountStrategy(int windowSize, long threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        this.requestCount = 0;
        this.exceptionsCount = 0;
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
            exceptionsCount++;
            if (exceptionsCount >= threshold) {
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
            exceptionsCount = 0;
            transition = HalfOpenTransition.NO_TRANSITION;
        } finally {
            lock.unlock();
        }
    }
}
