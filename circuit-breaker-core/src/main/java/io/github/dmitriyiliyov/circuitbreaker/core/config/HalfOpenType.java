package io.github.dmitriyiliyov.circuitbreaker.core.config;

/**
 * Defines the traffic routing strategy during the HALF_OPEN state.
 */
public enum HalfOpenType {

    /**
     * The standard strategy that permits a fixed number of trial requests.
     * The circuit transitions to CLOSED or OPEN based on the result of this fixed batch.
     */
    NORMAL,

    /**
     * A progressive strategy that gradually increases the permitted traffic percentage
     * upon successful requests, preventing sudden load spikes on a recovering system.
     */
    GRADUAL
}
