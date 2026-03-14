package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;

/**
 * A provider for a specific observation strategy (e.g., for close, open, or half-open states).
 * <p>
 * Implementations of this interface are responsible for creating a strategy instance
 * that matches the given {@link CircuitBreakerConfiguration}.
 */
public interface StrategyProvider {

    /**
     * Returns the type of circuit breaker state this provider supports.
     *
     * @return the supported {@link CircuitStateType}
     */
    CircuitStateType getStateType();

    /**
     * Checks if this provider can create a strategy for the given configuration.
     *
     * @param configuration the circuit breaker configuration
     * @return {@code true} if the configuration is supported, {@code false} otherwise
     */
    boolean supports(CircuitBreakerConfiguration configuration);

    /**
     * Creates and returns a strategy instance based on the provided configuration.
     *
     * @param configuration the circuit breaker configuration
     * @return a configured strategy object
     */
    Object getStrategy(CircuitBreakerConfiguration configuration);
}
