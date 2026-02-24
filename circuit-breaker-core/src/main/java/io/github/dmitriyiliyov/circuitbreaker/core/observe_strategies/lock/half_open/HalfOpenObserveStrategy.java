package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open;

/**
 * An observation strategy for the HALF_OPEN state.
 * <p>
 * This strategy determines whether the CircuitBreaker should transition to OPEN or CLOSED.
 */
public interface HalfOpenObserveStrategy {

    /**
     * Records a successful request.
     */
    void onRequest();

    /**
     * Records a failed request.
     */
    void onException();

    /**
     * Returns the recommended transition for the CircuitBreaker.
     *
     * @return the transition to perform
     */
    HalfOpenTransition getTransition();

    /**
     * Resets the internal state of the strategy.
     */
    void reset();
}
