package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
