package io.github.dmitriyiliyov.circuitbreaker.example;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.ExceptionPriority;
import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreaker circuitBreaker(CircuitBreakerFactory circuitBreakerFactory) {
        CircuitBreakerConfiguration configuration = CircuitBreakerConfiguration.builder()
                .name("exampleCircuitBreaker")
                .observableExceptions(Set.of(SpecificBusinessException.class))
                .ignorableExceptions(Set.of(IllegalArgumentException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .maxRequestExecutionDuration(Duration.ofMillis(100))
                .lockFree(false)
                .closeState(closeState ->
                        closeState.windowSize(10)
                                .exceptionRateThreshold(0.1)
                                .initialDelay(Duration.ofMinutes(1))
                )
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .halfOpenState(halfOpenState -> halfOpenState
                        .type(HalfOpenType.NORMAL)
                        .maxRequestInHalfOpenState(20)
                        .maxExceptionCountInHalfOpenState(2)
                )
                .build();
        return circuitBreakerFactory.create(configuration);
    }
}
