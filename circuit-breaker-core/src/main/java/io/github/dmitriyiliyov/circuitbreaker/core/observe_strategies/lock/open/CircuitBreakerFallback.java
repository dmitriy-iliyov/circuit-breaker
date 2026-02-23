package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

public interface CircuitBreakerFallback {
    <T> T get();
}
