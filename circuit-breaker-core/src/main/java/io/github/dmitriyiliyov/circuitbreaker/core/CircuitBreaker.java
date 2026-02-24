package io.github.dmitriyiliyov.circuitbreaker.core;

import java.util.Set;

/**
 * A CircuitBreaker abstraction.
 */
public interface CircuitBreaker extends CircuitState {

    /**
     * Returns the set of exceptions that are observed by the CircuitBreaker.
     *
     * @return the set of observable exceptions
     */
    Set<Class<? extends Throwable>> getObservableExceptions();

    /**
     * Attempts to transition the CircuitBreaker state from {@code previousState} to {@code nextState}.
     * This method is thread-safe.
     *
     * @param previousState the expected current state
     * @param nextState     the new state to transition to
     * @return              true if the transition was successful, false otherwise
     */
    boolean trySetState(CircuitState previousState, CircuitState nextState);

    /**
     * Returns the current state of the CircuitBreaker.
     *
     * @return the current state
     */
    CircuitState getState();
}
