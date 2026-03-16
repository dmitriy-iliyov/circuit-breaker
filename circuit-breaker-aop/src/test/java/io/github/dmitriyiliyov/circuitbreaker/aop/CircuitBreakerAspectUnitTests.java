package io.github.dmitriyiliyov.circuitbreaker.aop;

import io.github.dmitriyiliyov.circuitbreaker.core.CheckedSupplier;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerOpenException;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CircuitBreakerAspectUnitTests {

    private final CircuitBreakerRegistry registry = mock(CircuitBreakerRegistry.class);
    private final CircuitBreakerAspect aspect = new CircuitBreakerAspect(registry);
    private final ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
    private final CircuitBreaker cba = mock(CircuitBreaker.class);
    private final io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker cb = mock(io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker.class);

    @BeforeEach
    void setUp() {
        reset(registry, jp, cba, cb);
        when(cba.name()).thenReturn("test-breaker");
    }

    @Test
    @DisplayName("UT: advice() should proceed when circuit breaker is found and closed")
    void advice_shouldProceed_whenCircuitBreakerFoundAndClosed() throws Throwable {
        when(registry.getCircuitBreaker("test-breaker")).thenReturn(cb);
        when(jp.proceed()).thenReturn("success");
        when(cb.execute(any(CheckedSupplier.class))).thenAnswer(invocation -> {
            CheckedSupplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        Object result = aspect.advice(jp, cba);

        assertThat(result).isEqualTo("success");
        verify(registry).getCircuitBreaker("test-breaker");
        verify(cb).execute(any(CheckedSupplier.class));
        verify(jp).proceed();
    }

    @Test
    @DisplayName("UT: advice() should throw CircuitBreakerNotFound when circuit breaker is not found")
    void advice_shouldThrowException_whenCircuitBreakerNotFound() {
        when(registry.getCircuitBreaker("test-breaker")).thenReturn(null);

        assertThatThrownBy(() -> aspect.advice(jp, cba))
                .isInstanceOf(CircuitBreakerNotFound.class)
                .hasMessage("Circuit breaker with name 'test-breaker' not found");

        verify(registry).getCircuitBreaker("test-breaker");
        verifyNoInteractions(jp, cb);
    }

    @Test
    @DisplayName("UT: advice() should propagate exception from proceed")
    void advice_shouldPropagateException_fromProceed() throws Throwable {
        when(registry.getCircuitBreaker("test-breaker")).thenReturn(cb);
        when(jp.proceed()).thenThrow(new RuntimeException("test error"));
        when(cb.execute(any(CheckedSupplier.class))).thenAnswer(invocation -> {
            CheckedSupplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        assertThatThrownBy(() -> aspect.advice(jp, cba))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test error");
    }

    @Test
    @DisplayName("UT: advice() should propagate exception from circuit breaker execute")
    void advice_shouldPropagateException_fromCircuitBreaker() throws Throwable {
        when(registry.getCircuitBreaker("test-breaker")).thenReturn(cb);
        when(cb.execute(any(CheckedSupplier.class))).thenThrow(new CircuitBreakerOpenException("breaker is open"));

        assertThatThrownBy(() -> aspect.advice(jp, cba))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("breaker is open");

        verifyNoInteractions(jp);
    }
}
