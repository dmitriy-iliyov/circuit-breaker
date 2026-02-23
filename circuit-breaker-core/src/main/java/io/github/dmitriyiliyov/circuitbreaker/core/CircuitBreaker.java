package io.github.dmitriyiliyov.circuitbreaker.core;

import java.util.Set;

public interface CircuitBreaker extends CircuitState {
    Set<Class<? extends Throwable>> getObservableExceptions();
    boolean trySetState(CircuitState previousState, CircuitState nestState);
    CircuitState getState();
}
