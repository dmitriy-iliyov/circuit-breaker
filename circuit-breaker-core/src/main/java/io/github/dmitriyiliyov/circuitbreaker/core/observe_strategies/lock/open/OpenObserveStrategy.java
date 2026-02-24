package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

/**
 * An observation strategy for the OPEN state.
 * <p>
 * This strategy determines when the CircuitBreaker should transition from OPEN to HALF_OPEN.
 */
public interface OpenObserveStrategy {

    /**
     * Records a request attempt.
     */
    void onRequest();

    /**
     * Checks if the CircuitBreaker should transition to the HALF_OPEN state.
     *
     * @return true if the CircuitBreaker should transition, false otherwise
     */
    boolean shouldTrip();

    /**
     * Resets the internal state of the strategy.
     */
    void reset();
}
