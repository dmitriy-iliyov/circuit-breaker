package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close.CloseObserveStrategy;
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
public class CloseStateUnitTests {

    @Mock
    CircuitBreaker circuitBreaker;

    @Mock
    CircuitState nextState;

    @Mock
    CloseObserveStrategy strategy;

    CloseState closeState;

    @BeforeEach
    public void setUp() {
        when(circuitBreaker.getObservableExceptions()).thenReturn(Set.of(IllegalArgumentException.class));
        closeState = new CloseState(circuitBreaker, nextState, strategy);
    }

    @Test
    @DisplayName("UT: execute(Runnable) should call strategy.onRequest() on success")
    public void executeRunnable_shouldCallOnRequest_onSuccess() {
        closeState.execute(() -> {});

        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: execute(Runnable) should call strategy.onException() when observable exception is thrown")
    public void executeRunnable_shouldCallOnException_whenObservableExceptionThrown() {
        assertThatThrownBy(() -> closeState.execute(() -> {
            throw new IllegalArgumentException();
        })).isInstanceOf(IllegalArgumentException.class);

        verify(strategy).onException();
        verify(strategy, never()).onRequest();
    }

    @Test
    @DisplayName("UT: execute(Runnable) should call strategy.onRequest() when unobservable exception is thrown")
    public void executeRunnable_shouldCallOnRequest_whenUnobservableExceptionThrown() {
        assertThatThrownBy(() -> closeState.execute(() -> {
            throw new IllegalStateException("Not observable");
        })).isInstanceOf(IllegalStateException.class);

        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should return value and call strategy.onRequest() on success")
    public void executeSupplier_shouldReturnValueAndCallOnRequest_onSuccess() {
        String result = closeState.execute(() -> "success");

        assertThat(result).isEqualTo("success");
        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should call strategy.onException() when observable exception is thrown")
    public void executeSupplier_shouldCallOnException_whenObservableExceptionThrown() {
        assertThatThrownBy(() -> closeState.execute((Supplier<String>) () -> {
            throw new IllegalArgumentException();
        })).isInstanceOf(IllegalArgumentException.class);

        verify(strategy).onException();
        verify(strategy, never()).onRequest();
    }

    @Test
    @DisplayName("UT: execute(Supplier) should call strategy.onRequest() when unobservable exception is thrown")
    public void executeSupplier_shouldCallOnRequest_whenUnobservableExceptionThrown() {
        assertThatThrownBy(() -> closeState.execute((Supplier<String>) () -> {
            throw new IllegalStateException("Not observable");
        })).isInstanceOf(IllegalStateException.class);

        verify(strategy).onRequest();
        verify(strategy, never()).onException();
    }

    @Test
    @DisplayName("UT: should switch state and reset strategy when shouldTrip returns true and trySetState succeeds")
    public void shouldSwitchStateAndResetStrategy_whenShouldTripTrueAndSetStateSucceeds() {
        when(strategy.shouldTrip()).thenReturn(true);
        when(circuitBreaker.trySetState(closeState, nextState)).thenReturn(true);

        closeState.execute(() -> {});

        verify(circuitBreaker).trySetState(closeState, nextState);
        verify(strategy).reset();
    }

    @Test
    @DisplayName("UT: should NOT reset strategy when shouldTrip returns true but trySetState fails")
    public void shouldNotResetStrategy_whenShouldTripTrueButSetStateFails() {
        when(strategy.shouldTrip()).thenReturn(true);
        when(circuitBreaker.trySetState(closeState, nextState)).thenReturn(false);

        closeState.execute(() -> {});

        verify(circuitBreaker).trySetState(closeState, nextState);
        verify(strategy, never()).reset();
    }

    @Test
    @DisplayName("UT: should NOT attempt to switch state when shouldTrip returns false")
    public void shouldNotAttemptSwitchState_whenShouldTripFalse() {
        when(strategy.shouldTrip()).thenReturn(false);

        closeState.execute(() -> {});

        verify(circuitBreaker, never()).trySetState(any(), any());
        verify(strategy, never()).reset();
    }
}
