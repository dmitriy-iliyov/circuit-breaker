package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FailFastFixedTimeWindowStrategy implements OpenObserveStrategy {

    private final Duration ttl;
    private Instant observeEnd;
    private boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FailFastFixedTimeWindowStrategy(Duration ttl) {
        this.ttl = ttl;
        this.shouldTrip = false;
        this.observeEnd = Instant.now().plus(ttl);
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            Instant now = Instant.now();
            if (now.isAfter(observeEnd)) {
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
            shouldTrip = false;
            observeEnd = Instant.now().plus(ttl);
        } finally {
            lock.unlock();
        }
    }
}
