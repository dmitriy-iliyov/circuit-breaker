package io.github.dmitriyiliyov.circuitbreaker.strategies;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedWindowErrorRateStrategy implements ObserveStrategy {

    private final Duration ttl;
    private final double threshold;
    private Instant observeEnd;
    private int involveCount;
    private int observableExceptionCount;

    public FixedWindowErrorRateStrategy(Duration ttl, double threshold) {
        this.ttl = ttl;
        this.threshold = threshold;
        reset();
    }

    @Override
    public void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback) {
        involveCount++;
        try {
            process.run();
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        involveCount++;
        try {
            return process.get();
        } catch (Exception e) {
            handelException(e, checker, callback);
            throw e;
        }
    }

    public void handelException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
        if (checker.apply(e)) {
            Instant now = Instant.now();
            if (observeEnd.isBefore(now)) {
                reset();
                involveCount++;
                observableExceptionCount++;
                return;
            }
            observableExceptionCount++;
            double currentFrequency = (double) observableExceptionCount / involveCount;
            if (currentFrequency >= threshold) {
                callback.run();
            }
        }
    }

    @Override
    public void reset() {
        observeEnd = Instant.now().plusSeconds(ttl.toSeconds());
        involveCount = 0;
        observableExceptionCount = 0;
    }
}
