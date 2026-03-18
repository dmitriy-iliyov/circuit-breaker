package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitState;

/**
 * Wrapper to measure the elapsed time of an operation.
 * <p>
 * Used to determine if a request has exceeded a specific time threshold.
 */
public interface RequestTimer extends CircuitState { }
