package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OpenStateUnitTests {

    private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
    private final CircuitState nextState = mock(CircuitState.class);
    private final OpenStateStrategy strategy = mock(OpenStateStrategy.class);
    private final OpenState openState = new OpenState(circuitBreaker, nextState, strategy);

    @BeforeEach
    void setUp() {
        reset(circuitBreaker, nextState, strategy);
    }

    @Test
    @DisplayName("execute(supplier): should throw exceptionSupplier when transition is not allowed")
    void supplier_execute_shouldThrowException_whenTransitionNotAllowed() {
        when(strategy.shouldTransition()).thenReturn(false);

        assertThatThrownBy(() -> openState.execute(() -> "result"))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("Circuit breaker is open, request cannot be executed");

        verify(strategy).onRequest();
        verify(strategy).shouldTransition();
        verifyNoMoreInteractions(strategy);
        verifyNoInteractions(circuitBreaker, nextState);
    }

    @Test
    @DisplayName("execute(supplier): should throw exceptionSupplier when state transition fails")
    void supplier_execute_shouldThrowException_whenStateTransitionFails() {
        when(strategy.shouldTransition()).thenReturn(true);
        when(circuitBreaker.trySetState(openState, nextState)).thenReturn(false);

        assertThatThrownBy(() -> openState.execute(() -> "result"))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("Circuit breaker is open, request cannot be executed");

        verify(strategy).onRequest();
        verify(strategy).shouldTransition();
        verify(circuitBreaker).trySetState(openState, nextState);
        verifyNoMoreInteractions(strategy, circuitBreaker);
        verifyNoInteractions(nextState);
    }

    @Test
    @DisplayName("execute(supplier): should execute process and return result on successful transition")
    void supplier_execute_shouldExecuteAndReturnResult_onSuccessfulTransition() throws Throwable {
        when(strategy.shouldTransition()).thenReturn(true);
        when(circuitBreaker.trySetState(openState, nextState)).thenReturn(true);

        String result = openState.execute(() -> "successSupplier");

        assertThat(result).isEqualTo("successSupplier");
        verify(strategy).onRequest();
        verify(strategy).shouldTransition();
        verify(circuitBreaker).trySetState(openState, nextState);
        verify(strategy).reset();
        verifyNoMoreInteractions(strategy, circuitBreaker);
        verifyNoInteractions(nextState);
    }

    @Test
    @DisplayName("execute(runnable): should throw exceptionSupplier when transition is not allowed")
    void runnable_execute_shouldThrowException_whenTransitionNotAllowed() {
        when(strategy.shouldTransition()).thenReturn(false);
        CheckedRunnable process = mock(CheckedRunnable.class);

        assertThatThrownBy(() -> openState.execute(process))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("Circuit breaker is open, request cannot be executed");

        verify(strategy).onRequest();
        verify(strategy).shouldTransition();
        verifyNoMoreInteractions(strategy);
        verifyNoInteractions(circuitBreaker, nextState, process);
    }

    @Test
    @DisplayName("execute(runnable): should throw exceptionSupplier when state transition fails")
    void runnable_execute_shouldThrowException_whenStateTransitionFails() {
        when(strategy.shouldTransition()).thenReturn(true);
        when(circuitBreaker.trySetState(openState, nextState)).thenReturn(false);
        CheckedRunnable process = mock(CheckedRunnable.class);

        assertThatThrownBy(() -> openState.execute(process))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessage("Circuit breaker is open, request cannot be executed");

        verify(strategy).onRequest();
        verify(strategy).shouldTransition();
        verify(circuitBreaker).trySetState(openState, nextState);
        verifyNoMoreInteractions(strategy, circuitBreaker);
        verifyNoInteractions(nextState, process);
    }

    @Test
    @DisplayName("execute(runnable): should execute process on successful transition")
    void runnable_execute_shouldExecute_onSuccessfulTransition() throws Throwable {
        when(strategy.shouldTransition()).thenReturn(true);
        when(circuitBreaker.trySetState(openState, nextState)).thenReturn(true);
        CheckedRunnable process = mock(CheckedRunnable.class);

        openState.execute(process);

        verify(process).run();
        verify(strategy).onRequest();
        verify(strategy).shouldTransition();
        verify(circuitBreaker).trySetState(openState, nextState);
        verify(strategy).reset();
        verifyNoMoreInteractions(strategy, circuitBreaker, process);
        verifyNoInteractions(nextState);
    }
}
