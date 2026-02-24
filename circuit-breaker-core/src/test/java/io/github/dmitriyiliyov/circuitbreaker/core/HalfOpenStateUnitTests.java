package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open.HalfOpenObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open.HalfOpenTransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HalfOpenStateUnitTests {

    @Mock
    CircuitBreaker circuitBreaker;

    @Mock
    CircuitState openState;

    @Mock
    CircuitState closeState;

    @Mock
    HalfOpenObserveStrategy strategy;

    HalfOpenState halfOpenState;

    @BeforeEach
    public void setUp() {
        when(circuitBreaker.getObservableExceptions()).thenReturn(Set.of(IllegalArgumentException.class));
        halfOpenState = new HalfOpenState(circuitBreaker, openState, closeState, strategy);
    }

    @Test
    @DisplayName("UT: execute(Runnable) should call strategy.onRequest() on success")
    public void executeRunnable_shouldCallOnRequest_onSuccess() {
        halfOpenState.execute(() -> {});

        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: execute(Runnable) should call strategy.onException() when observable exception is thrown")
    public void executeRunnable_shouldCallOnException_whenObservableExceptionThrown() {
        assertThatThrownBy(() -> halfOpenState.execute(() -> {
            throw new IllegalArgumentException();
        })).isInstanceOf(IllegalArgumentException.class);

        verify(strategy).onException();
        verify(strategy, never()).onRequest();
    }

    @Test
    @DisplayName("UT: execute(Runnable) should call strategy.onRequest() when unobservable exception is thrown")
    public void executeRunnable_shouldCallOnRequest_whenUnobservableExceptionThrown() {
        assertThatThrownBy(() -> halfOpenState.execute(() -> {
            throw new IllegalStateException("Not observable");
        })).isInstanceOf(IllegalStateException.class);

        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should return value and call strategy.onRequest() on success")
    public void executeSupplier_shouldReturnValueAndCallOnRequest_onSuccess() {
        String result = halfOpenState.execute(() -> "success");

        assertThat(result).isEqualTo("success");
        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should call strategy.onException() when observable exception is thrown")
    public void executeSupplier_shouldCallOnException_whenObservableExceptionThrown() {
        assertThatThrownBy(() -> halfOpenState.execute((Supplier<String>) () -> {
            throw new IllegalArgumentException();
        })).isInstanceOf(IllegalArgumentException.class);

        verify(strategy).onException();
        verify(strategy, never()).onRequest();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should call strategy.onRequest() when unobservable exception is thrown")
    public void executeSupplier_shouldCallOnRequest_whenUnobservableExceptionThrown() {
        assertThatThrownBy(() -> halfOpenState.execute((Supplier<String>) () -> {
            throw new IllegalStateException("Not observable");
        })).isInstanceOf(IllegalStateException.class);

        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: should switch to OPEN state and reset strategy when transition is TO_OPEN and trySetState succeeds")
    public void shouldSwitchToOpenStateAndResetStrategy_whenTransitionToOpenAndSetStateSucceeds() {
        when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
        when(circuitBreaker.trySetState(halfOpenState, openState)).thenReturn(true);

        halfOpenState.execute(() -> {});

        verify(circuitBreaker).trySetState(halfOpenState, openState);
        verify(strategy).reset();
    }

    @Test
    @DisplayName("UT: should switch to CLOSE state and reset strategy when transition is TO_CLOSE and trySetState succeeds")
    public void shouldSwitchToCloseStateAndResetStrategy_whenTransitionToCloseAndSetStateSucceeds() {
        when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
        when(circuitBreaker.trySetState(halfOpenState, closeState)).thenReturn(true);

        halfOpenState.execute(() -> {});

        verify(circuitBreaker).trySetState(halfOpenState, closeState);
        verify(strategy).reset();
    }

    @Test
    @DisplayName("UT: should NOT reset strategy when transition is TO_OPEN but trySetState fails")
    public void shouldNotResetStrategy_whenTransitionToOpenButSetStateFails() {
        when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
        when(circuitBreaker.trySetState(halfOpenState, openState)).thenReturn(false);

        halfOpenState.execute(() -> {});

        verify(circuitBreaker).trySetState(halfOpenState, openState);
        verify(strategy, never()).reset();
    }

    @Test
    @DisplayName("UT: should NOT reset strategy when transition is TO_CLOSE but trySetState fails")
    public void shouldNotResetStrategy_whenTransitionToCloseButSetStateFails() {
        when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
        when(circuitBreaker.trySetState(halfOpenState, closeState)).thenReturn(false);

        halfOpenState.execute(() -> {});

        verify(circuitBreaker).trySetState(halfOpenState, closeState);
        verify(strategy, never()).reset();
    }

    @Test
    @DisplayName("UT: should NOT attempt to switch state when transition is NO_TRANSITION")
    public void shouldNotAttemptSwitchState_whenTransitionNoTransition() {
        when(strategy.getTransition()).thenReturn(HalfOpenTransition.NO_TRANSITION);

        halfOpenState.execute(() -> {});

        verify(circuitBreaker, never()).trySetState(any(), any());
        verify(strategy, never()).reset();
    }
}
