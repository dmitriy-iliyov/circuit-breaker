package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CloseStateUnitTests {

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private CircuitState nextState;

    @Mock
    private CloseStateStrategy strategy;

    private CloseState closeState;

    @BeforeEach
    void setUp() {
        when(circuitBreaker.getChecker()).thenReturn(throwable -> throwable instanceof IllegalArgumentException);
    }

    @Nested
    @DisplayName("Tests for execute and transition logic")
    class ExecuteAndTransitionTests {

        @BeforeEach
        void setUp() {
            closeState = new CloseState(circuitBreaker, nextState, strategy);
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onSuccess() on successSupplier")
        void executeRunnable_shouldCallOnSuccess_onSuccess() throws Throwable {
            closeState.execute(() -> {});
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onException() when observable exceptionSupplier is thrown")
        void executeRunnable_shouldCallOnException_whenObservableExceptionThrown() {
            assertThatThrownBy(() -> closeState.execute(() -> {
                throw new IllegalArgumentException();
            })).isInstanceOf(IllegalArgumentException.class);
            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onSuccess() when unobservable exceptionSupplier is thrown")
        void executeRunnable_shouldCallOnSuccess_whenUnobservableExceptionThrown() {
            assertThatThrownBy(() -> closeState.execute(() -> {
                throw new IllegalStateException("Not observable");
            })).isInstanceOf(IllegalStateException.class);
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should return value and call strategy.onSuccess() on successSupplier")
        void executeSupplier_shouldReturnValueAndCallOnSuccess_onSuccess() throws Throwable {
            String result = closeState.execute(() -> "successSupplier");
            assertThat(result).isEqualTo("successSupplier");
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should call strategy.onException() when observable exceptionSupplier is thrown")
        void executeSupplier_shouldCallOnException_whenObservableExceptionThrown() {
            assertThatThrownBy(() -> closeState.execute((CheckedSupplier<String>) () -> {
                throw new IllegalArgumentException();
            })).isInstanceOf(IllegalArgumentException.class);
            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should call strategy.onSuccess() when unobservable exceptionSupplier is thrown")
        void executeSupplier_shouldCallOnSuccess_whenUnobservableExceptionThrown() {
            assertThatThrownBy(() -> closeState.execute((CheckedSupplier<String>) () -> {
                throw new IllegalStateException("Not observable");
            })).isInstanceOf(IllegalStateException.class);
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: should switch state when strategy shouldTrip and trySetState succeeds")
        void shouldSwitchState_whenShouldTripAndSetStateSucceeds() throws Throwable {
            when(strategy.shouldTrip()).thenReturn(true);
            when(circuitBreaker.trySetState(closeState, nextState)).thenReturn(true);
            closeState.execute(() -> {});
            verify(circuitBreaker).trySetState(closeState, nextState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("UT: should NOT reset strategy when shouldTrip is true but trySetState fails")
        void shouldNotResetStrategy_whenShouldTripButSetStateFails() throws Throwable {
            when(strategy.shouldTrip()).thenReturn(true);
            when(circuitBreaker.trySetState(closeState, nextState)).thenReturn(false);
            closeState.execute(() -> {});
            verify(circuitBreaker).trySetState(closeState, nextState);
            verify(strategy, never()).reset();
        }

        @Test
        @DisplayName("UT: should NOT attempt to switch state when shouldTrip is false")
        void shouldNotAttemptSwitchState_whenShouldTripFalse() throws Throwable {
            when(strategy.shouldTrip()).thenReturn(false);
            closeState.execute(() -> {});
            verify(circuitBreaker, never()).trySetState(any(), any());
            verify(strategy, never()).reset();
        }
    }

    @Nested
    @DisplayName("Tests for setter logic")
    class SetterTests {

        @BeforeEach
        void setUp() {
            closeState = new CloseState(circuitBreaker, strategy);
        }

        @Test
        @DisplayName("setNextState should set state when not initialized")
        void setNextState_shouldSetState_whenNotInitialized() {
            closeState.setNextState(nextState);
            assertThat(closeState.getNextState()).isSameAs(nextState);
        }

        @Test
        @DisplayName("setNextState should throw NullPointerException when state is null")
        void setNextState_shouldThrowNPE_whenStateIsNull() {
            assertThatThrownBy(() -> closeState.setNextState(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("nextState cannot be null");
        }

        @Test
        @DisplayName("setNextState should throw IllegalStateException when already initialized")
        void setNextState_shouldThrowIllegalState_whenAlreadyInitialized() {
            closeState.setNextState(nextState);
            assertThatThrownBy(() -> closeState.setNextState(mock(CircuitState.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("cannot modify state with this method");
        }
    }
}
