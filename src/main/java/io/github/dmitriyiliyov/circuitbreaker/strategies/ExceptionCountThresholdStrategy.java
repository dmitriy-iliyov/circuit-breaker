package io.github.dmitriyiliyov.circuitbreaker.strategies;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExceptionCountThresholdStrategy implements ObserveStrategy {

    private final Duration ttl;
    private final long threshold;
    private final Map<String, String> exceptions = new LinkedHashMap<>();

    public ExceptionCountThresholdStrategy(Duration ttl, long threshold) {
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
