package io.github.dmitriyiliyov.circuitbreaker.strategies;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExceptionFrequencyThresholdStrategy implements ObserveStrategy {

    private final Duration ttl;
    private final double threshold;

    public ExceptionFrequencyThresholdStrategy(Duration ttl, double threshold) {
        this.ttl = ttl;
        this.threshold = threshold;
    }

    @Override
    public void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback) {

    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        return null;
    }

    @Override
    public void reset() {

    }
}
