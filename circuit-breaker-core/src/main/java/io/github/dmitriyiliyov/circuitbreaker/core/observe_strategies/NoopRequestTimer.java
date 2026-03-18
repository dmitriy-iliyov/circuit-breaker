package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import io.github.dmitriyiliyov.circuitbreaker.core.CheckedRunnable;
import io.github.dmitriyiliyov.circuitbreaker.core.CheckedSupplier;

public class NoopRequestTimer implements RequestTimer {

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        process.run();
    }

    @Override
    public <T> T execute(CheckedSupplier<T> process) throws Throwable {
        return process.get();
    }
}
