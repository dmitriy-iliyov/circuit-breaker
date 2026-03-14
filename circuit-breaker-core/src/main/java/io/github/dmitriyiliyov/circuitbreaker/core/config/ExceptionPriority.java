package io.github.dmitriyiliyov.circuitbreaker.core.config;

/**
 * Defines the priority for resolving conflicts when an exception matches both
 * observable and ignorable criteria.
 */
public enum ExceptionPriority {

    /**
     * Observable exceptions take precedence. If an exception is both observable and ignorable,
     * it will be treated as observable (counted as a failure).
     * Shared exceptions are removed from the ignorable set.
     */
    OBSERVABLE,

    /**
     * Ignorable exceptions take precedence. If an exception is both observable and ignorable,
     * it will be treated as ignorable (not counted as a failure).
     * Shared exceptions are removed from the observable set.
     */
    IGNORABLE
}
