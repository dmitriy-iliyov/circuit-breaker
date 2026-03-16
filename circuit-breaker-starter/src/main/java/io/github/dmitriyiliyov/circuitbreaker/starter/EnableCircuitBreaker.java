package io.github.dmitriyiliyov.circuitbreaker.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CircuitBreakerAutoConfiguration.class)
public @interface EnableCircuitBreaker { }
