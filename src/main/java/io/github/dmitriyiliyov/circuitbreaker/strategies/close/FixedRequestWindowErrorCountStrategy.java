package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedRequestWindowErrorCountStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final long threshold;
    private final Lock lock = new ReentrantLock();
    private int currentRequestCount;
    private int exceptionsCount;

    public FixedRequestWindowErrorCountStrategy(int windowSize, long threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
        reset();
    }

    @Override
    public void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback) {
        handleRequest();
        try {
            process.run();
        } catch (Exception e) {
            handleException(e, checker, callback);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        handleRequest();
        try {
            return process.get();
        } catch (Exception e) {
            handleException(e, checker, callback);
            throw e;
        }
    }

    private void handleRequest() {
        lock.lock();
        try {
            currentRequestCount++;
            if (currentRequestCount >= windowSize) {
                reset();
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
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
