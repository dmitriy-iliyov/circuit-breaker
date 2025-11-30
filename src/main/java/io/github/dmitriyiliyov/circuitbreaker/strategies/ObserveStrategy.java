package io.github.dmitriyiliyov.circuitbreaker.strategies;

import io.github.dmitriyiliyov.circuitbreaker.Resettable;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ObserveStrategy extends Resettable {
    void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback);
    <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback);
}
