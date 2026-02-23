package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
