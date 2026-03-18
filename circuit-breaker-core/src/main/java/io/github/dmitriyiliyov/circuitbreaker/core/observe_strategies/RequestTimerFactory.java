package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;

public final class RequestTimerFactory {

    private RequestTimerFactory() {}

    public static RequestTimer of(CircuitBreakerConfiguration configuration) {
        if (configuration.isRequestTimerEnable()) {
            return new DefaultRequestTimer(configuration.getMaxRequestExecutionDuration());
        }
        return new NoopRequestTimer();
    }
}
