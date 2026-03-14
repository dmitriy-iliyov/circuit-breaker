package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

/**
 * Represents the possible states of a circuit breaker.
 */
public enum CircuitStateType {
    /**
     * The circuit is closed, and requests are allowed to pass through.
     */
    CLOSE,
    /**
     * The circuit is partially open, allowing a limited number of trial requests.
     */
    HALF_OPEN,
    /**
     * The circuit is open, and requests are blocked.
     */
    OPEN
}
