package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

/**
 * Represents the possible states of a circuit breaker.
 */
public enum CircuitStateType {
    CLOSE, HALF_OPEN, OPEN
}
