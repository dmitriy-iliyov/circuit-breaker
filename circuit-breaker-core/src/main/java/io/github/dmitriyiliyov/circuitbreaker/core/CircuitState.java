package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * A CircuitBreaker state abstraction.
 */
public interface CircuitState {

    /**
     * Executes the given {@code process} within the context of this CircuitBreaker state.
     *
     * @param process the process to execute
     * @throws        Throwable if the process throws an exception
     */
    void execute(CheckedRunnable process) throws Throwable;

    /**
     * Executes the given {@code process} within the context of this CircuitBreaker state.
     *
     * @param process the process to execute
     * @param <T>     the type of the return value of the process
     * @return        the return value of the process
     * @throws        Throwable if the process throws an exception
     */
    <T> T execute(CheckedSupplier<T> process) throws Throwable;
}
