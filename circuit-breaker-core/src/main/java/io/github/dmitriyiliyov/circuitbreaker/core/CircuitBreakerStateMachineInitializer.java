package io.github.dmitriyiliyov.circuitbreaker.core;

/**
 * Assembles the state machine for a {@link CircuitBreaker}.
 * <p>
 * Connects the {@link CloseState}, {@link OpenState}, and {@link HalfOpenState}
 * and sets the initial state on the circuit breaker.
 */
public final class CircuitBreakerStateMachineInitializer {

    /**
     * Assembles a three-state machine (Close, Open, Half-Open).
     *
     * @param circuitBreaker The circuit breaker to configure.
     * @param strategies     The strategies for each state.
     */
    public static void init(ConfigurableCircuitBreaker circuitBreaker, Strategies strategies) {
        CircuitState halfOpenState = new HalfOpenState(
                (CircuitBreaker) circuitBreaker,
                strategies.halfOpenStateStrategy()
        );

        CircuitState openState = new OpenState(
                (CircuitBreaker) circuitBreaker,
                halfOpenState,
                strategies.openStateStrategy()
        );

        CircuitState closeState = new CloseState(
                (CircuitBreaker) circuitBreaker,
                openState,
                strategies.closeStateStrategy()
        );

        ((ConfigurableHalfOpenState) halfOpenState).setCloseState(closeState);
        ((ConfigurableHalfOpenState) halfOpenState).setOpenState(openState);

        circuitBreaker.setState(closeState);
    }

    /**
     * Assembles a two-state machine (Close, Open), skipping the Half-Open state.
     *
     * @param circuitBreaker The circuit breaker to configure.
     * @param strategies     The strategies for the open and close states.
     */
    public static void initWithoutHalfOpenState(ConfigurableCircuitBreaker circuitBreaker, Strategies strategies) {
        CircuitState closeState = new CloseState(
                (CircuitBreaker) circuitBreaker,
                strategies.closeStateStrategy()
        );

        CircuitState openState = new OpenState(
                (CircuitBreaker) circuitBreaker,
                closeState,
                strategies.openStateStrategy()
        );

        ((ConfigurableCircuitState) closeState).setNextState(openState);

        circuitBreaker.setState(closeState);
    }
}
