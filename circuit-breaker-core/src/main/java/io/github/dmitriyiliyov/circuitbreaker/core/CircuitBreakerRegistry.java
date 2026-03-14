package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;

/**
 * A central registry for {@link CircuitBreaker} instances.
 * <p>
 * Provides a way to manage and access circuit breakers throughout an application.
 */
public interface CircuitBreakerRegistry {

    /**
     * Registers a {@link CircuitBreaker} with its configuration.
     * <p>
     * If a circuit breaker with the same name (from the configuration) is already registered,
     * this method does nothing.
     *
     * @param configuration the configuration for the circuit breaker
     * @param circuitBreaker the circuit breaker instance to register
     */
    void register(CircuitBreakerConfiguration configuration, CircuitBreaker circuitBreaker);

    /**
     * Retrieves the {@link CircuitBreakerConfiguration} for a given circuit breaker name.
     *
     * @param name the name of the circuit breaker
     * @return the configuration, or {@code null} if not found
     */
    CircuitBreakerConfiguration getConfiguration(String name);

    /**
     * Retrieves a {@link CircuitBreaker} instance by its name.
     *
     * @param name the name of the circuit breaker
     * @return the circuit breaker instance, or {@code null} if not found
     */
    CircuitBreaker getCircuitBreaker(String name);
}
