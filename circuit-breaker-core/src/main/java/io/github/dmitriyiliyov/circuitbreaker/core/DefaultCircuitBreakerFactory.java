package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.StrategiesProvider;

public final class DefaultCircuitBreakerFactory implements CircuitBreakerFactory {

    private final CircuitBreakerRegistry registry;
    private final StrategiesProvider strategiesProvider;

    public DefaultCircuitBreakerFactory(CircuitBreakerRegistry registry, StrategiesProvider strategiesProvider) {
        this.registry = registry;
        this.strategiesProvider = strategiesProvider;
    }

    @Override
    public CircuitBreaker of(CircuitBreakerConfiguration configuration) {
        CircuitBreaker circuitBreaker = internalOf(configuration);
        registry.register(configuration, circuitBreaker);
        return circuitBreaker;
    }

    private CircuitBreaker internalOf(CircuitBreakerConfiguration configuration) {
        ConfigurableCircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
                configuration.getObservableExceptions(),
                configuration.getIgnorableExceptions()
        );
        Strategies strategies = strategiesProvider.getStrategies(configuration);
        if (configuration.isHalfOpenStateEnabled()) {
            CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategies);
        } else {
            CircuitBreakerStateMachineInitializer.initWithoutHalfOpenState(circuitBreaker, strategies);
        }
        return (CircuitBreaker) circuitBreaker;
    }

    @Override
    public CircuitBreaker ofExists(String referenceName, String newName) {
        CircuitBreakerConfiguration configuration = registry.getConfiguration(referenceName);
        CircuitBreakerConfiguration newConfiguration = configuration.toBuilder().name(newName).build();
        CircuitBreaker circuitBreaker = internalOf(newConfiguration);
        registry.register(newConfiguration, circuitBreaker);
        return circuitBreaker;
    }
}
