package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.RequestTimerFactory;
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
        if (configuration.isHalfOpenStateEnabled()) {
            CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategies, requestTimer);
        } else {
            CircuitBreakerStateMachineInitializer.initWithoutHalfOpenState(circuitBreaker, strategies, requestTimer);
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
