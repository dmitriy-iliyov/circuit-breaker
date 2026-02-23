package io.github.dmitriyiliyov.circuitbreaker.core;

import java.util.function.Supplier;

public interface CircuitState {
    void execute(Runnable process);
    <T> T execute(Supplier<T> process);
}
