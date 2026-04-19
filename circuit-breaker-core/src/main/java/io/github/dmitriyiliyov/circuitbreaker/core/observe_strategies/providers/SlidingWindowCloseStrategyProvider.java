package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CloseStateConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlidingWindowCloseStrategy;

import java.util.Objects;

public class SlidingWindowCloseStrategyProvider implements StrategyProvider {

    @Override
    public CircuitStateType getStateType() {
        return CircuitStateType.CLOSE;
    }

    @Override
    public boolean supports(CircuitBreakerConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        CloseStateConfiguration closeStateConfiguration = configuration.getCloseStateConfiguration();
        Objects.requireNonNull(closeStateConfiguration, "close state configuration cannot be null");
        return !configuration.getLockFree() &&
                closeStateConfiguration.getWindowSize() != null &&
                closeStateConfiguration.getExceptionCountThreshold() != null &&
                closeStateConfiguration.getInitialDelay() != null;
    }

    @Override
    public Object getStrategy(CircuitBreakerConfiguration configuration) {
        CloseStateConfiguration closeStateConfiguration = configuration.getCloseStateConfiguration();
        if (supports(configuration)) {
            return new SlidingWindowCloseStrategy(
                    closeStateConfiguration.getWindowSize(),
                    closeStateConfiguration.getExceptionCountThreshold(),
                    closeStateConfiguration.getInitialDelay()
            );
        }
        throw new IllegalArgumentException("configuration %s don't supports".formatted(configuration));
    }
}
