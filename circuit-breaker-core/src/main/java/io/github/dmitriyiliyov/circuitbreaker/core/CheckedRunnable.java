package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * A functional interface similar to {@link Runnable}, but allows throwing a {@link Throwable}.
 * This is used to wrap code that may throw exceptions within the circuit breaker.
 */
@FunctionalInterface
public interface CheckedRunnable {

    /**
     * Executes the action that can throw a {@link Throwable}.
     *
     * @throws Throwable if an error occurs during execution
     */
    void run() throws Throwable;
}
