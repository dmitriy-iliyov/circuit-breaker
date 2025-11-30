package io.github.dmitriyiliyov.circuitbreaker;

import java.util.Set;
import java.util.function.Supplier;

public interface CircuitBreaker {
    void process(Runnable process);

    <T> T process(Supplier<T> process);

    Set<Class<?>> getObservableExceptions();

    void setState(CircuitState state);
}
