package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.strategies.ObserveStrategy;

import java.util.function.Supplier;

public class RedirectStrategy implements ObserveStrategy {

    @Override
    public void observe(Runnable process) {

    }

    @Override
    public <T> T observe(Supplier<T> process) {
        return null;
    }

    @Override
    public void reset() {

    }
}
