package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;

/**
 * A factory for creating and managing {@link CircuitBreaker} instances.
 */
public interface CircuitBreakerFactory {

    /**
     * Creates a new {@link CircuitBreaker} from the given configuration.
     *
     * @param configuration The configuration to use.
     * @return A new {@link CircuitBreaker} instance.
     */
    CircuitBreaker create(CircuitBreakerConfiguration configuration);

    /**
     * Creates a new {@link CircuitBreaker} that shares the state of an existing one.
     *
     * @param referenceName The name of the existing circuit breaker to reference.
     * @param newName       The name for the new circuit breaker.
     * @return A new {@link CircuitBreaker} that shares the state of the reference.
     */
    CircuitBreaker ofExists(String referenceName, String newName);
}
