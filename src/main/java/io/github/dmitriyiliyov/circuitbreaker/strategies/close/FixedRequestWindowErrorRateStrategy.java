package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedRequestWindowErrorRateStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final double threshold;
    private int involveCount;
    private int observableExceptionCount;
    private Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorRateStrategy(int windowSize, double threshold) {
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
            handelException(e, checker, callback);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        handleRequest();
        try {
            return process.get();
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    public void handleRequest() {
        lock.lock();
        try {
            involveCount++;
            if (involveCount >= windowSize) {
                reset();
            }
        } finally {
            lock.unlock();
        }
    }

    public void handelException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
        lock.lock();
        try {
            if (!checker.apply(e)) {
                return;
            }
            observableExceptionCount++;
            if (involveCount > 0) {
                double currentFrequency = (double) observableExceptionCount / involveCount;
                if (currentFrequency >= threshold) {
                    callback.run();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        involveCount = 0;
        observableExceptionCount = 0;
    }
}
