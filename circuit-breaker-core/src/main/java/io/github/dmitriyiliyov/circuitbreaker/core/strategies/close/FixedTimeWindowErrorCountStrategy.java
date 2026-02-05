package io.github.dmitriyiliyov.circuitbreaker.core.strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedTimeWindowErrorCountStrategy implements ObserveStrategy {

    private final Duration ttl;
    private final Function<Exception, Boolean> checker;
    private Instant observeEnd;
    private final long threshold;
    private final Runnable callback;
    private long exceptionsCount;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorCountStrategy(Duration ttl, Function<Exception, Boolean> checker, long threshold,
                                             Runnable callback) {
        this.ttl = ttl;
        this.checker = checker;
        this.threshold = threshold;
        this.callback = callback;
        reset();
    }

    @Override
    public void observe(Runnable process) {
        try {
            process.run();
        } catch (Exception e) {
            handleException(e);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process) {
        try {
            return process.get();
        } catch (Exception e) {
            handleException(e);
            throw e;
        }
    }

    private void handleException(Exception e) {
        if (!checker.apply(e)) {
            return;
        }
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
                reset();
            }
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
        observeEnd = Instant.now().plus(ttl);
        exceptionsCount = 0;
    }
}
