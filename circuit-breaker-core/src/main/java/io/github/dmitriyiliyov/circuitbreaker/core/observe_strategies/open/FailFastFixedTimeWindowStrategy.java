package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link OpenObserveStrategy} that transitions to HALF_OPEN after a fixed time window.
 */
public class FailFastFixedTimeWindowStrategy implements OpenObserveStrategy {

    private final long observeMillis;
    private volatile long observeEndMillis;

    public FailFastFixedTimeWindowStrategy(Duration observeTime) {
        Objects.requireNonNull(observeTime, "observeTime cannot be null");
        this.observeMillis = observeTime.toMillis();
        this.observeEndMillis = System.currentTimeMillis() + observeMillis;
    }

    @Override
    public void onRequest() { }

    @Override
    public boolean shouldTrip() {
        return System.currentTimeMillis() >= observeEndMillis;
    }

    @Override
    public void reset() {
        observeEndMillis = System.currentTimeMillis() + observeMillis;
    }
}
