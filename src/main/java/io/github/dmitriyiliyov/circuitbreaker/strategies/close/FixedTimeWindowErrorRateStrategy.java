package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedTimeWindowErrorRateStrategy implements CloseObserveStrategy {

    private final Duration ttl;
    private final double threshold;
    private Instant observeEnd;
    private int involveCount;
    private int observableExceptionCount;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorRateStrategy(Duration ttl, double threshold) {
        this.ttl = ttl;
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

    public void handleRequest() {
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
                reset();
            }
            involveCount++;
        } finally {
            lock.unlock();
        }
    }

    public void handleException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
        if (!checker.apply(e)) {
            return;
        }
        lock.lock();
        try {
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
        observeEnd = Instant.now().plus(ttl);
        involveCount = 0;
        observableExceptionCount = 0;
    }
}
