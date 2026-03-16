package io.github.dmitriyiliyov.circuitbreaker.aop;

/**
 * An exception thrown when a {@link io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker}
 * instance could not be found in the {@link io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerRegistry}.
 */
public class CircuitBreakerNotFound extends RuntimeException {
    public CircuitBreakerNotFound(String name) {
        super("Circuit breaker with name '%s' not found".formatted(name));
    }
}
