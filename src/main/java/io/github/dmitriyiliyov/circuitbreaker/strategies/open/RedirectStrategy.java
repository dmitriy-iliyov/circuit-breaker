package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

import java.util.function.Supplier;

public class RedirectStrategy implements OpenObserveStrategy {

    @Override
    public void observe(Runnable process, Runnable callback) {

    }

    @Override
    public <T> T observe(Supplier<T> process, Runnable callback) {
        return null;
    }

    @Override
    public void reset() {

    }
}
