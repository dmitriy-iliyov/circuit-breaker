package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenStateConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.LockFreeCountBasedHalfOpenStrategy;

import java.util.Objects;

public class LockFreeCountBasedHalfOpenStrategyProvider implements StrategyProvider {

    @Override
    public CircuitStateType getStateType() {
        return CircuitStateType.HALF_OPEN;
    }

    @Override
    public boolean supports(CircuitBreakerConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        HalfOpenStateConfiguration halfOpenStateConfiguration = Objects.requireNonNull(
                configuration.getHalfOpenStateConfiguration(), "halfOpenStateConfiguration cannot be null"
        );
        if (!halfOpenStateConfiguration.isHalfOpenStateEnabled()) {
            return false;
        }
        return configuration.getLockFree() &&
                halfOpenStateConfiguration.getMaxRequestInHalfOpenState() > 0 &&
                halfOpenStateConfiguration.getMaxExceptionCountInHalfOpenState() >= 0;
    }

    @Override
    public Object getStrategy(CircuitBreakerConfiguration configuration) {
        if (supports(configuration)) {
            return new LockFreeCountBasedHalfOpenStrategy(
                    configuration.getHalfOpenStateConfiguration().getMaxRequestInHalfOpenState(),
                    configuration.getHalfOpenStateConfiguration().getMaxExceptionCountInHalfOpenState()
            );
        }
        throw new IllegalArgumentException("configuration %s don't supports".formatted(configuration));
    }
}
