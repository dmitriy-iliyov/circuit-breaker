package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * An interface for configuring the internal state of a {@link HalfOpenState}.
 * This is primarily used during the initialization of the circuit breaker to link the half-open state
 * with the other states.
 */
public interface ConfigurableHalfOpenState {

    /**
     * Initializes the close state reference.
     * <p>
     * This method can only be called once to initialize the state if it wasn't set in the constructor.
     * Subsequent calls will throw a {@link IllegalStateException}.
     *
     * @param closeState the close state to transition to
     * @throws NullPointerException if closeState is null
     * @throws IllegalStateException if the close state has already been initialized
     */
    void setCloseState(CircuitState closeState);

    /**
     * Initializes the open state reference.
     * <p>
     * This method can only be called once to initialize the state if it wasn't set in the constructor.
     * Subsequent calls will throw a {@link IllegalStateException}.
     *
     * @param openState the open state to transition to
     * @throws NullPointerException if openState is null
     * @throws IllegalStateException if the open state has already been initialized
     */
    void setOpenState(CircuitState openState);
}
