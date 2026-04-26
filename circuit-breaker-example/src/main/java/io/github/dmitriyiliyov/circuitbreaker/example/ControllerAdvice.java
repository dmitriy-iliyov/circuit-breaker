package io.github.dmitriyiliyov.circuitbreaker.example;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerOpenException;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlowRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = BusinessController.class)
public class ControllerAdvice {

    @ExceptionHandler(SpecificBusinessException.class)
    public ProblemDetail handleSpecificBusinessException() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Observable by circuit breaker business exception"
        );
    }

    @ExceptionHandler(SlowRequestException.class)
    public ProblemDetail handleSlowRequestException() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.REQUEST_TIMEOUT,
                "Request was to slow"
        );
    }

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ProblemDetail handleCircuitBreakerOpenException() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Circuit breaker is open"
        );
    }
}
