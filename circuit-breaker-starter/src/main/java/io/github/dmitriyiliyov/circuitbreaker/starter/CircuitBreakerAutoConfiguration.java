package io.github.dmitriyiliyov.circuitbreaker.starter;


import io.github.dmitriyiliyov.circuitbreaker.aop.CircuitBreakerAspect;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerRegistry;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerRegistry;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CircuitBreakerAutoConfiguration {

    private static final List<StrategyProvider> DEFAULT_PROVIDERS = List.of(
            new SlidingWindowCloseStrategyProvider(),
            new LockFreeSlidingWindowCloseStrategyProvider(),
            new TimeBasedOpenStrategyProvider(),
            new CountBasedHalfOpenStrategyProvider(),
            new LockFreeCountBasedHalfOpenStrategyProvider()
    );

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return new DefaultCircuitBreakerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public StrategiesProvider strategiesProvider(List<StrategyProvider> providers) {
        if (!providers.isEmpty()) {
            return new DefaultStrategiesProvider(providers);
        }
        return new DefaultStrategiesProvider(DEFAULT_PROVIDERS);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
                                                       StrategiesProvider strategiesProvider) {
        return new DefaultCircuitBreakerFactory(circuitBreakerRegistry, strategiesProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerAspect(circuitBreakerRegistry);
    }
}
