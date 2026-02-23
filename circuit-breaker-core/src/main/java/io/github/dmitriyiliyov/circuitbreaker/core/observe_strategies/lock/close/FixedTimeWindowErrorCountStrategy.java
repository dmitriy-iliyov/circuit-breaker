package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedTimeWindowErrorCountStrategy implements CloseObserveStrategy {

    private final Duration ttl;
    private Instant observeEnd;
    private final long threshold;
    private long exceptionsCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorCountStrategy(Duration ttl, long threshold) {
        this.ttl = ttl;
        this.threshold = threshold;
        this.observeEnd = Instant.now().plus(ttl);
        this.exceptionsCount = 0;
        this.shouldTrip = false;
        throw new IllegalStateException("Shouldn't use Instant, should use System.milis ???");
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
                observeEnd = Instant.now().plus(ttl);
                exceptionsCount = 0;
                shouldTrip = false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onException() {
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
                observeEnd = Instant.now().plus(ttl);
                exceptionsCount = 0;
                shouldTrip = false;
            }
            exceptionsCount++;
            if (exceptionsCount >= threshold) {
                shouldTrip = true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean shouldTrip() {
        return shouldTrip;
    }

    @Override
    public void reset() {
        lock.lock();
        try {
            observeEnd = Instant.now().plus(ttl);
            exceptionsCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
