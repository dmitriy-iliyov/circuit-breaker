package io.github.dmitriyiliyov.circuitbreaker.core.strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedRequestWindowErrorCountStrategy implements ObserveStrategy {

    private final int windowSize;
    private final Function<Exception, Boolean> checker;
    private final long threshold;
    private final Runnable callback;
    private final Lock lock = new ReentrantLock();
    private int currentRequestCount;
    private int exceptionsCount;

    public FixedRequestWindowErrorCountStrategy(int windowSize, Function<Exception, Boolean> checker, long threshold,
                                                Runnable callback) {
        this.windowSize = windowSize;
        this.checker = checker;
        this.threshold = threshold;
        this.callback = callback;
        reset();
    }

    @Override
    public void observe(Runnable process) {
        handleRequest();
        try {
            process.run();
        } catch (Exception e) {
            handleException(e);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process) {
        handleRequest();
        try {
            return process.get();
        } catch (Exception e) {
            handleException(e);
            throw e;
        }
    }

    private void handleRequest() {
        lock.lock();
        try {
            currentRequestCount++;
            if (currentRequestCount > windowSize) {
                reset();
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleException(Exception e) {
        if (!checker.apply(e)) {
            return;
        }
        lock.lock();
        try {
            exceptionsCount++;
            if (exceptionsCount >= threshold) {
                callback.run();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        currentRequestCount = 0;
        exceptionsCount = 0;
    }
}
