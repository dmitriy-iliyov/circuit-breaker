package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimerFactory;
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
        CircuitBreaker circuitBreaker = createFromConfiguration(configuration);
        registry.register(configuration, circuitBreaker);
        return circuitBreaker;
    }

    private CircuitBreaker createFromConfiguration(CircuitBreakerConfiguration configuration) {
        ConfigurableCircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
                configuration.getObservableExceptions(),
                configuration.getIgnorableExceptions()
        );
        Strategies strategies = strategiesProvider.getStrategies(configuration);
        RequestTimer requestTimer = RequestTimerFactory.of(configuration);
        if (configuration.getHalfOpenStateConfiguration().isHalfOpenStateEnabled()) {
            CircuitBreakerStateMachineInitializer.initWithHalfOpen(
                    circuitBreaker, strategies, requestTimer, configuration.getHalfOpenStateConfiguration()
            );
        } else {
            CircuitBreakerStateMachineInitializer.initWithoutHalfOpen(circuitBreaker, strategies, requestTimer);
        }
        return (CircuitBreaker) circuitBreaker;
    }

    @Override
    public CircuitBreaker ofExists(String referenceName, String newName) {
        CircuitBreakerConfiguration configuration = registry.getConfiguration(referenceName);
        CircuitBreakerConfiguration newConfiguration = configuration.toBuilder().name(newName).build();
        CircuitBreaker circuitBreaker = createFromConfiguration(newConfiguration);
        registry.register(newConfiguration, circuitBreaker);
        return circuitBreaker;
    }
}
