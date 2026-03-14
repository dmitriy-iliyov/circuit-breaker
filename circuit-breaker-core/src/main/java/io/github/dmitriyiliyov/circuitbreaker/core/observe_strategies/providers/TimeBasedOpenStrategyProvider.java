package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.TimeBasedOpenStrategy;

import java.util.Objects;

public class TimeBasedOpenStrategyProvider implements StrategyProvider {

    @Override
    public CircuitStateType getStateType() {
        return CircuitStateType.OPEN;
    }

    @Override
    public boolean supports(CircuitBreakerConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        return configuration.getWaitDurationInOpenState() != null;
    }

    @Override
    public Object getStrategy(CircuitBreakerConfiguration configuration) {
        if (supports(configuration)) {
            return new TimeBasedOpenStrategy(configuration.getWaitDurationInOpenState());
        }
        throw new IllegalArgumentException("configuration %s don't supports".formatted(configuration));
    }
}
