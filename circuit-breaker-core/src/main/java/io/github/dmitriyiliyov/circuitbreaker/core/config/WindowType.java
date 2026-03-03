package io.github.dmitriyiliyov.circuitbreaker.core.config;

/**
 * Defines how the observation window moves.
 */
public enum WindowType {
    /**
     * The window is fixed and resets after a certain period or count.
     */
    FIXED,
    /**
     * The window slides continuously, dropping old data as new data arrives.
     */
    SLIDING
}
