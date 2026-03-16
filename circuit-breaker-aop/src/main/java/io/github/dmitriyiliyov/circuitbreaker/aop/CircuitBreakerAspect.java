package io.github.dmitriyiliyov.circuitbreaker.aop;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Objects;

/**
 * An AspectJ aspect that intercepts methods annotated with {@link CircuitBreaker}.
 * <p>
 * This aspect wraps the execution of the annotated method with the logic of a
 * {@link io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker}.
 */
@Aspect
public class CircuitBreakerAspect {

    public final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = Objects.requireNonNull(circuitBreakerRegistry, "circuit breaker registry cannot be null");
    }

    /**
     * The advice that wraps the annotated method.
     *
     * @param jp  the join point representing the method execution
     * @param cba the {@link CircuitBreaker} annotation instance
     * @return the result of the original method's execution
     * @throws Throwable if the original method or the circuit breaker throws an exception
     */
    @Around(value = "@annotation(cba)")
    public Object advice(ProceedingJoinPoint jp, CircuitBreaker cba) throws Throwable {
        io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker cb = circuitBreakerRegistry.getCircuitBreaker(cba.name());
        if (cb == null) {
            throw new CircuitBreakerNotFound(cba.name());
        }
        return cb.execute(() -> jp.proceed());
    }
}
