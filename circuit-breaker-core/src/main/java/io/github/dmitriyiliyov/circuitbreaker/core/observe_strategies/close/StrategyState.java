package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import java.util.concurrent.atomic.AtomicInteger;

public record StrategyState(
        AtomicInteger requests,
        AtomicInteger exceptions
) {
    public static StrategyState of() {
        return new StrategyState(new AtomicInteger(0), new AtomicInteger(0));
    }

    public static StrategyState of(int requests, int exceptions) {
        return new StrategyState(new AtomicInteger(requests), new AtomicInteger(exceptions));
    }
}
