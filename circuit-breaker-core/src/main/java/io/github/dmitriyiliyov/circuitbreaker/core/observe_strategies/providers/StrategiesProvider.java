package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.Strategies;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;

/**
 * A provider for creating a set of observation strategies for a circuit breaker.
 * <p>
 * This interface is responsible for assembling the {@link Strategies} object
 * based on a given {@link CircuitBreakerConfiguration}.
 */
public interface StrategiesProvider {

    /**
     * Creates and returns a {@link Strategies} object based on the provided configuration.
     *
     * @param configuration the circuit breaker configuration
     * @return a configured {@link Strategies} object
     */
    Strategies getStrategies(CircuitBreakerConfiguration configuration);
}
