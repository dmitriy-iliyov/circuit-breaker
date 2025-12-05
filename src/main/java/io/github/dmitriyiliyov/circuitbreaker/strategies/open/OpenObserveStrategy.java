package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.Resettable;

import java.util.function.Supplier;

public interface OpenObserveStrategy extends Resettable {
    void observe(Runnable process, Runnable callback);
    <T> T observe(Supplier<T> process, Runnable callback);
}
