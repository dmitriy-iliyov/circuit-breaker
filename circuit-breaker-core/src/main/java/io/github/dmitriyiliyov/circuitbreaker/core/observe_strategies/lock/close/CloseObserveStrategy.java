package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

/**
 * An observation strategy for the CLOSED state.
 * <p>
 * This strategy determines when the CircuitBreaker should transition from CLOSED to OPEN.
 */
public interface CloseObserveStrategy {

    /**
     * Records a successful request.
     */
    void onRequest();

    /**
     * Records a failed request.
     */
    void onException();

    /**
     * Checks if the CircuitBreaker should transition to the OPEN state.
     *
     * @return true if the CircuitBreaker should transition, false otherwise
     */
    boolean shouldTrip();

    /**
     * Resets the internal state of the strategy.
     */
    void reset();
}
