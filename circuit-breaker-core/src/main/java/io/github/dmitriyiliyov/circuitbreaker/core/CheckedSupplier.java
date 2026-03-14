package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * A functional interface similar to {@link java.util.function.Supplier}, but allows throwing a {@link Throwable}.
 * This is used to wrap code that may throw exceptions and returns a value within the circuit breaker.
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

    /**
     * Gets a result, potentially throwing an exception.
     *
     * @return a result
     * @throws Throwable if an error occurs during execution
     */
    T get() throws Throwable;
}
