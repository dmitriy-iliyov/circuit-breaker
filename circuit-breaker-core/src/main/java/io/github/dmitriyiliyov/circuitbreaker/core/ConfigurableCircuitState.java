package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * An interface for configuring the internal state of a {@link CircuitState}.
 * This is primarily used during the initialization of the circuit breaker to link states together.
 */
interface ConfigurableCircuitState {

    /**
     * Sets the next state to transition to from the current state.
     *
     * @param nextState the next {@link CircuitState}
     */
    void setNextState(CircuitState nextState);
}
