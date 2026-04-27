package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenStateConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenType;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;

/**
 * A utility class for assembling the state machine of a {@link CircuitBreaker}.
 * <p>
 * This class links the different states ({@link CloseState}, {@link OpenState}, {@link HalfOpenState} or {@link GradualHalfOpenState})
 * together and sets the initial state on the circuit breaker instance.
 */
final class CircuitBreakerStateMachineInitializer {

    private CircuitBreakerStateMachineInitializer() {}

    /**
     * Initializes a standard three-state machine (Close -> Open -> Half-Open -> Close).
     *
     * @param circuitBreaker               The circuit breaker instance to initialize.
     * @param strategies                   A container with the strategies for each state.
     * @param requestTimer                 A timer to measure request duration, used by some strategies.
     * @param halfOpenStateConfiguration   Half-Open state configuration, used to get half-open state type and type specific data.
     */
    public static void initWithHalfOpen(ConfigurableCircuitBreaker circuitBreaker,
                                        Strategies strategies,
                                        RequestTimer requestTimer,
                                        HalfOpenStateConfiguration halfOpenStateConfiguration) {
        CircuitState halfOpenState;
        HalfOpenType halfOpenType = halfOpenStateConfiguration.getType();
        if (HalfOpenType.NORMAL.equals(halfOpenType)) {
            halfOpenState = new HalfOpenState(
                    (CircuitBreaker) circuitBreaker,
                    strategies.halfOpenStateStrategy(),
                    requestTimer
            );
        } else if (HalfOpenType.GRADUAL.equals(halfOpenType)) {
            halfOpenState = new GradualHalfOpenState(
                    (CircuitBreaker) circuitBreaker,
                    strategies.halfOpenStateStrategy(),
                    requestTimer,
                    halfOpenStateConfiguration.getMultiplier()
            );
        } else {
            throw new IllegalStateException(
                    "Unreached state detected; '%s' is unknown HalfOpenType".formatted(halfOpenType)
            );
        }

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
    public static void initWithoutHalfOpen(ConfigurableCircuitBreaker circuitBreaker,
                                           Strategies strategies,
                                           RequestTimer requestTimer) {
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
