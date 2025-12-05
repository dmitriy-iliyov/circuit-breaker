package io.github.dmitriyiliyov.circuitbreaker.strategies.half_open;

import io.github.dmitriyiliyov.circuitbreaker.Resettable;

import java.util.function.Function;
import java.util.function.Supplier;

public interface HalfOpenObserveStrategy extends Resettable {
    void observe(Runnable process, Function<Exception, Boolean> checker, Runnable callback);
    <T> T observe(Supplier<T> process, Function<Exception, Boolean> checker, Runnable callback);
}
