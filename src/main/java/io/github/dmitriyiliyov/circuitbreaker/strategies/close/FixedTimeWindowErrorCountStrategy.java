package io.github.dmitriyiliyov.circuitbreaker.strategies.close;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

public class FixedTimeWindowErrorCountStrategy implements CloseObserveStrategy {

    private final Duration ttl;
    private Instant observeEnd;
    private final long threshold;
    private long exceptionsCount;

    public FixedTimeWindowErrorCountStrategy(Duration ttl, long threshold) {
        this.ttl = ttl;
        this.threshold = threshold;
        reset();
    }

    @Override
    public void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback) {
        try {
            process.run();
        } catch (Exception e) {
            handleException(e, checker, callback);
            throw e;
        }
    }

    @Override
    public <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback) {
        try {
            return process.get();
        } catch (Exception e) {
            handleException(e, checker, callback);
            throw e;
        }
    }

    private void handleException(Exception e, Function<Exception, Boolean> checker, Runnable callback) {
        if (!checker.apply(e)) {
            return;
        }
        Instant now = Instant.now();
        if (now.isAfter(observeEnd)) {
            reset();
        }
        exceptionsCount++;
        if (exceptionsCount >= threshold) {
            callback.run();
        }
    }

    @Override
    public void reset() {
        observeEnd = Instant.now().plus(ttl);
        exceptionsCount = 0;
    }
}
