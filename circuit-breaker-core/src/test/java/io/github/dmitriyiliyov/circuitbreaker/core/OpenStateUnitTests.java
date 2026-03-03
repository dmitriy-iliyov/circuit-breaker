package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CircuitBreakerOpenException;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OpenStateUnitTests {

    @Mock
    CircuitBreaker circuitBreaker;

    @Mock
    CircuitState nextState;

    @Mock
    OpenObserveStrategy strategy;

    OpenState openState;

    @BeforeEach
    public void setUp() {
        openState = new OpenState(circuitBreaker, nextState, strategy);
    }

    @Test
    @DisplayName("UT: execute(Runnable) should throw CircuitBreakerOpenException and call strategy.onRequest()")
    public void executeRunnable_shouldThrowExceptionAndCallOnRequest() {
        assertThatThrownBy(() -> openState.execute(() -> {}))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("Circuit breaker is open, request cannot be executed");

        verify(strategy).onRequest();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should throw CircuitBreakerOpenException and call strategy.onRequest()")
    public void executeSupplier_shouldThrowExceptionAndCallOnRequest() {
        assertThatThrownBy(() -> openState.execute((Supplier<String>) () -> "success"))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("Circuit breaker is open, request cannot be executed");

        verify(strategy).onRequest();
    }

    @Test
    @DisplayName("UT: should switch state and reset strategy when shouldTrip returns true and trySetState succeeds")
    public void shouldSwitchStateAndResetStrategy_whenShouldTripTrueAndSetStateSucceeds() {
        when(strategy.shouldTrip()).thenReturn(true);
        when(circuitBreaker.trySetState(openState, nextState)).thenReturn(true);

        assertThrows(CircuitBreakerOpenException.class, () -> openState.execute(() -> {}));

        verify(circuitBreaker).trySetState(openState, nextState);
        verify(strategy).reset();
    }

    @Test
    @DisplayName("UT: should NOT reset strategy when shouldTrip returns true but trySetState fails")
    public void shouldNotResetStrategy_whenShouldTripTrueButSetStateFails() {
        when(strategy.shouldTrip()).thenReturn(true);
        when(circuitBreaker.trySetState(openState, nextState)).thenReturn(false);

        assertThrows(CircuitBreakerOpenException.class, () -> openState.execute(() -> {}));

        verify(circuitBreaker).trySetState(openState, nextState);
        verify(strategy, never()).reset();
    }

    @Test
    @DisplayName("UT: should NOT attempt to switch state when shouldTrip returns false")
    public void shouldNotAttemptSwitchState_whenShouldTripFalse() {
        when(strategy.shouldTrip()).thenReturn(false);

        assertThrows(CircuitBreakerOpenException.class, () -> openState.execute(() -> {}));

        verify(circuitBreaker, never()).trySetState(any(), any());
        verify(strategy, never()).reset();
    }
}
