package io.github.dmitriyiliyov.circuitbreaker.benchmark;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerRegistry;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.*;

import java.util.List;
import java.util.function.Supplier;

final class BenchmarkSupport {

    static final CircuitBreakerFactory FACTORY = new DefaultCircuitBreakerFactory(
            new DefaultCircuitBreakerRegistry(),
            new DefaultStrategiesProvider(List.of(
                    new SlidingWindowCloseStrategyProvider(),
                    new LockFreeSlidingWindowCloseStrategyProvider(),
                    new TimeBasedOpenStrategyProvider(),
                    new CountBasedHalfOpenStrategyProvider(),
                    new LockFreeCountBasedHalfOpenStrategyProvider())
            )
    );

    private BenchmarkSupport() {}

    static String workload(int loopLimit) {
        int sum = 0;
        for (int i = 0; i < loopLimit; i++) {
            sum += i;
        }
        return "ok" + sum;
    }

    static String executeMy(CircuitBreaker cb, Supplier<String> supplier) {
        try {
            return cb.execute(supplier::get);
        } catch (Throwable t) {
            return "fallback";
        }
    }
}
