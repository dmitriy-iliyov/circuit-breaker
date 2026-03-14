package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
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
        if (!configuration.isHalfOpenStateEnabled()) {
            return false;
        }
        return configuration.getLockFree() &&
                configuration.getMaxRequestInHalfOpenState() > 0 &&
                configuration.getMaxExceptionCountInHalfOpenState() >= 0;
    }

    @Override
    public Object getStrategy(CircuitBreakerConfiguration configuration) {
        if (supports(configuration)) {
            return new LockFreeCountBasedHalfOpenStrategy(
                    configuration.getMaxRequestInHalfOpenState(),
                    configuration.getMaxExceptionCountInHalfOpenState()
            );
        }
        throw new IllegalArgumentException("configuration %s don't supports".formatted(configuration));
    }
}
