package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

/**
 * Possible transitions from the HALF_OPEN state.
 */
public enum HalfOpenTransition {
    /**
     * No transition should be made. The CircuitBreaker remains in the HALF_OPEN state.
     * <p>
     * This is the case when not enough requests have been recorded to make a decision.
     */
    NO_TRANSITION,

    /**
     * The CircuitBreaker should transition to the CLOSED state.
     * <p>
     * This is the case when the success rate is above the threshold.
     */
    TO_CLOSE,

    /**
     * The CircuitBreaker should transition to the OPEN state.
     * <p>
     * This is the case when the failure rate is above the threshold.
     */
    TO_OPEN
}
