package io.github.dmitriyiliyov.circuitbreaker.core.strategies.open;

public interface CircuitBreakerFallback {
    <T> T get();
}
