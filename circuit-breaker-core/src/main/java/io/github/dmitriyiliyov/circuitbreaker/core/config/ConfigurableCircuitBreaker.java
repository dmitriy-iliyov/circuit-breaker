package io.github.dmitriyiliyov.circuitbreaker.core.config;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitState;

public interface ConfigurableCircuitBreaker {
    void setState(CircuitState state);
}
