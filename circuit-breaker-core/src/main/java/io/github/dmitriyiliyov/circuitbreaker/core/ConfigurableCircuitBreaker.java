package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * Interface for configuring a Circuit Breaker instance.
 * <p>
 * This interface provides methods to set the internal state of the circuit breaker,
 * which is primarily used during initialization or testing.
 */
public interface ConfigurableCircuitBreaker {

    /**
     * Initializes the circuit breaker state.
     * <p>
     * This method can only be called once to initialize the state if it wasn't set in the constructor.
     * Subsequent calls will throw a {@link IllegalStateException}.
     *
     * @param state the initial state
     * @throws IllegalStateException if the state has already been initialized
     */
    void setState(CircuitState state);
}
