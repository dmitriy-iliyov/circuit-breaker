package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
