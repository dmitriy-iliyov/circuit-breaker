package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCircuitBreakerRegistry implements CircuitBreakerRegistry {

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreakerConfiguration> circuitBreakerConfigurations = new ConcurrentHashMap<>();

    @Override
    public void register(CircuitBreakerConfiguration configuration, CircuitBreaker circuitBreaker) {
        circuitBreakers.putIfAbsent(configuration.getName(), circuitBreaker);
        circuitBreakerConfigurations.putIfAbsent(configuration.getName(), configuration);
    }

    @Override
    public CircuitBreakerConfiguration getConfiguration(String name) {
        return circuitBreakerConfigurations.get(name);
    }

    @Override
    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakers.get(name);
    }
}
