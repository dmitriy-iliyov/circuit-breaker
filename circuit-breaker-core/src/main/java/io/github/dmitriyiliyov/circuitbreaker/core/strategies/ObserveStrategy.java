package io.github.dmitriyiliyov.circuitbreaker.core.strategies;

import io.github.dmitriyiliyov.circuitbreaker.core.Resettable;

import java.util.function.Supplier;

public interface ObserveStrategy extends Resettable {
    void observe(Runnable process);
    <T> T observe(Supplier<T> process);
}
