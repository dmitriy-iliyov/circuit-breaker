package io.github.dmitriyiliyov.circuitbreaker.core;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
