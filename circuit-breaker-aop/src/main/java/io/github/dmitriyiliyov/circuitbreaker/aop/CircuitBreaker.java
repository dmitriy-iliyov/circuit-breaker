package io.github.dmitriyiliyov.circuitbreaker.aop;

import java.lang.annotation.*;

/**
 * An annotation to protect a method with a Circuit Breaker.
 * <p>
 * The annotated method will be executed through a {@link io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker}
 * instance retrieved from the {@link io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerRegistry}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CircuitBreaker {

    /**
     * The name of the circuit breaker to use, as registered in the
     * {@link io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerRegistry}.
     *
     * @return the name of the circuit breaker
     */
    String name();
}
