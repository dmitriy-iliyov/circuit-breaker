package io.github.dmitriyiliyov.circuitbreaker.core.strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedTimeWindowErrorRateStrategy implements ObserveStrategy {

    private final Duration ttl;
    private final Function<Exception, Boolean> checker;
    private final double threshold;
    private Instant observeEnd;
    private final Runnable callback;
    private int currentRequestCount;
    private int observableExceptionCount;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorRateStrategy(Duration ttl, Function<Exception, Boolean> checker, double threshold,
                                            Runnable callback) {
        this.ttl = ttl;
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

    public void handleRequest() {
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
                reset();
            }
            currentRequestCount++;
        } finally {
            lock.unlock();
        }
    }

    public void handleException(Exception e) {
        if (!checker.apply(e)) {
            return;
        }
        lock.lock();
        try {
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
        observeEnd = Instant.now().plus(ttl);
        currentRequestCount = 0;
        observableExceptionCount = 0;
    }
}
