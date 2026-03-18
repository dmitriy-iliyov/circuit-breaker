package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;

/**
 * A utility class for assembling the state machine of a {@link CircuitBreaker}.
 * <p>
 * This class links the different states ({@link CloseState}, {@link OpenState}, {@link HalfOpenState})
 * together and sets the initial state on the circuit breaker instance.
 */
public final class CircuitBreakerStateMachineInitializer {

    private CircuitBreakerStateMachineInitializer() {}

    /**
     * Initializes a standard three-state machine (Close -> Open -> Half-Open -> Close).
     *
     * @param circuitBreaker The circuit breaker instance to initialize.
     * @param strategies     A container with the strategies for each state.
     * @param requestTimer   A timer to measure request duration, used by some strategies.
     */
    public static void init(ConfigurableCircuitBreaker circuitBreaker, Strategies strategies, RequestTimer requestTimer) {
        CircuitState halfOpenState = new HalfOpenState(
                (CircuitBreaker) circuitBreaker,
                strategies.halfOpenStateStrategy(),
                requestTimer
        );

        CircuitState openState = new OpenState(
                (CircuitBreaker) circuitBreaker,
                halfOpenState,
                strategies.openStateStrategy()
        );

        CircuitState closeState = new CloseState(
                (CircuitBreaker) circuitBreaker,
                openState,
                strategies.closeStateStrategy(),
                requestTimer
        );

        ((ConfigurableHalfOpenState) halfOpenState).setCloseState(closeState);
        ((ConfigurableHalfOpenState) halfOpenState).setOpenState(openState);

        circuitBreaker.setState(closeState);
    }

    /**
     * Initializes a simplified two-state machine (Close -> Open -> Close), skipping the Half-Open state.
     *
     * @param circuitBreaker The circuit breaker instance to initialize.
     * @param strategies     A container with the strategies for the open and close states.
     * @param requestTimer   A timer to measure request duration, used by some strategies.
     */
    public static void initWithoutHalfOpenState(ConfigurableCircuitBreaker circuitBreaker, Strategies strategies, RequestTimer requestTimer) {
        CircuitState closeState = new CloseState(
                (CircuitBreaker) circuitBreaker,
                strategies.closeStateStrategy(),
                requestTimer
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
