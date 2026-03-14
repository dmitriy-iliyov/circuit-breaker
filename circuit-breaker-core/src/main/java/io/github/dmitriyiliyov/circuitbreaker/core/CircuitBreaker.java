package io.github.dmitriyiliyov.circuitbreaker.core;

import java.util.Set;
import java.util.function.Function;

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
     * Returns the set of exceptions that are ignored by the CircuitBreaker.
     *
     * @return the set of ignorable exceptions
     */
    Set<Class<? extends Throwable>> getIgnorableExceptions();

    /**
     * Returns a function that checks if an exception should be counted as a failure.
     * Ignorable exceptions take precedence over observable ones.
     *
     * @return true if the exception is observable and not ignored, false otherwise
     */
    Function<Throwable, Boolean> getChecker();

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
