package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import java.time.Duration;

/**
 * An {@link OpenStateStrategy} that transitions to CLOSE after a fixed time window.
 */
public class TimeBasedOpenStrategy implements OpenStateStrategy {

    private final long observeMillis;
    private volatile long observeEndMillis;

    public TimeBasedOpenStrategy(Duration observeTime) {
        this.observeMillis = observeTime.toMillis();
        this.observeEndMillis = System.currentTimeMillis() + observeMillis;
    }

    @Override
    public void onRequest() { }

    @Override
    public boolean shouldTransition() {
        return System.currentTimeMillis() >= observeEndMillis;
    }

    @Override
    public void reset() {
        observeEndMillis = System.currentTimeMillis() + observeMillis;
    }
}
