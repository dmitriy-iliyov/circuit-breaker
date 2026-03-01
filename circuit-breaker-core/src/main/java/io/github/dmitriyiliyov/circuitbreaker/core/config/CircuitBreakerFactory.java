package io.github.dmitriyiliyov.circuitbreaker.core.config;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;

public final class CircuitBreakerFactory {

    public static CircuitBreaker of(CircuitBreakerConfiguration configuration) {
//        CircuitBreakerConfiguration configurationa = CircuitBreakerConfiguration.builder()
//                .observableExceptions(Set.of(HttpTimeoutException.class))
//                .ignorableExceptions(Set.of(HttpConnectTimeoutException.class))
//                .lockFree(true)
//                .closeState(close -> close.windowMoveType(WindowType.MOVING)
//                        .windowSize(100)
//                        .exceptionRateThreshold(0.1)
//                )
//                .halfOpenState(halfOpen -> halfOpen.observeTime(Duration.ofMinutes(3))
//                        .exceptionCountThreshold(5)
//                )
//                .openState(open -> open.observeTime(Duration.ofMinutes(2)))
//                .build();
        return null;
    }
}
