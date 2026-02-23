package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedTimeWindowErrorRateStrategy implements CloseObserveStrategy {

    private final Duration ttl;
    private final double threshold;
    private Instant observeEnd;
    private int requestCount;
    private int exceptionCount;
    private volatile boolean shouldTrip;
    private final Lock lock = new ReentrantLock();

    public FixedTimeWindowErrorRateStrategy(Duration ttl, double threshold) {
        this.ttl = ttl;
        this.threshold = threshold;
        this.observeEnd = Instant.now().plus(ttl);
        this.requestCount = 0;
        this.exceptionCount = 0;
        this.shouldTrip = false;
        throw new IllegalStateException("Shouldn't use Instant, should use System.milis ???");
    }

    @Override
    public void onRequest() {
        lock.lock();
        try {
            Instant now = Instant.now();
            requestCount++;
            if (now.isAfter(observeEnd)) {
                observeEnd = Instant.now().plus(ttl);
                requestCount = 1;
                exceptionCount = 0;
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
            requestCount++;
            if (now.isAfter(observeEnd)) {
                observeEnd = Instant.now().plus(ttl);
                requestCount = 1;
                exceptionCount = 0;
                shouldTrip = false;
            }
            exceptionCount++;
            double frequency = (double) exceptionCount / requestCount;
            if (frequency >= threshold) {
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
            requestCount = 0;
            exceptionCount = 0;
            shouldTrip = false;
        } finally {
            lock.unlock();
        }
    }
}
