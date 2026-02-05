package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
