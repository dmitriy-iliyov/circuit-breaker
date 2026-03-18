package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.DefaultRequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.NoopRequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;

public final class RequestTimerFactory {

    private RequestTimerFactory() {}

    public static RequestTimer of(CircuitBreakerConfiguration configuration) {
        if (configuration.isRequestTimerEnable()) {
            return new DefaultRequestTimer(configuration.getMaxRequestExecutionDuration());
        }
        return new NoopRequestTimer();
    }
}
