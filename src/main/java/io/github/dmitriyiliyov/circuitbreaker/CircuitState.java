package io.github.dmitriyiliyov.circuitbreaker;

import java.util.function.Supplier;

public interface CircuitState extends Resettable {
    void process(Runnable process);
    <T> T process(Supplier<T> process);
}
