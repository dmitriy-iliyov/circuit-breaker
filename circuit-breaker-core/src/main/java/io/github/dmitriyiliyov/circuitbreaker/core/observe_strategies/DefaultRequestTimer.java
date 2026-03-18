package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import io.github.dmitriyiliyov.circuitbreaker.core.CheckedRunnable;
import io.github.dmitriyiliyov.circuitbreaker.core.CheckedSupplier;

import java.time.Duration;

public class DefaultRequestTimer implements RequestTimer {

    private final long maxExecutionNanos;

    public DefaultRequestTimer(Duration maxExecutionTime) {
        this.maxExecutionNanos = maxExecutionTime.toNanos();
    }

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        long start = System.nanoTime();
        process.run();
        if (System.nanoTime() - start >= maxExecutionNanos) {
            throw new SlowRequestException("The request was terminated because it was too slow");
        }
    }

    @Override
    public <T> T execute(CheckedSupplier<T> process) throws Throwable {
        long start = System.nanoTime();
        T result = process.get();
        if (System.nanoTime() - start >= maxExecutionNanos) {
            throw new SlowRequestException("The request was terminated because it was too slow");
        }
        return result;
    }
}
