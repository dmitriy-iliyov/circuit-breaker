package io.github.dmitriyiliyov.circuitbreaker.core.strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedRequestWindowErrorRateStrategy implements ObserveStrategy {

    private final int windowSize;
    private final Function<Exception, Boolean> checker;
    private final double threshold;
    private final Runnable callback;
    private int currentRequestCount;
    private int observableExceptionCount;
    private Lock lock = new ReentrantLock();

    public FixedRequestWindowErrorRateStrategy(int windowSize, Function<Exception, Boolean> checker, long threshold,
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
            handelException(e);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process) {
        try {
            return process.get();
        } catch (Exception e) {
            handelException(e);
            throw e;
        }
    }

    public void handleRequest() {
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

    public void handelException(Exception e) {
        lock.lock();
        try {
            if (!checker.apply(e)) {
                return;
            }
            observableExceptionCount++;
            if (currentRequestCount > 0) {
                double currentFrequency = (double) observableExceptionCount / currentRequestCount;
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
        currentRequestCount = 0;
        observableExceptionCount = 0;
    }
}
