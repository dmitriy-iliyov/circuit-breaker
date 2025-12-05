package io.github.dmitriyiliyov.circuitbreaker.strategies.open;

public interface CircuitBreakerFallback {
    <T> T get();
}
