package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenTransition;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlowRequestException;
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
public class HalfOpenStateUnitTests {

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private CircuitState openState;

    @Mock
    private CircuitState closeState;

    @Mock
    private HalfOpenStateStrategy strategy;

    @Mock
    private RequestTimer timer;

    private HalfOpenState halfOpenState;

    @BeforeEach
    void setUp() {
        when(circuitBreaker.getChecker()).thenReturn(throwable -> throwable instanceof IllegalArgumentException);
    }

    @Nested
    @DisplayName("Tests for execute and transition logic")
    class ExecuteAndTransitionTests {

        @BeforeEach
        void setUp() throws Throwable {
            halfOpenState = new HalfOpenState(circuitBreaker, openState, closeState, strategy, timer);
            lenient().doAnswer(invocation -> {
                invocation.<CheckedRunnable>getArgument(0).run();
                return null;
            }).when(timer).execute(any(CheckedRunnable.class));

            lenient().doAnswer(invocation ->
                    invocation.<CheckedSupplier<?>>getArgument(0).get()
            ).when(timer).execute(any(CheckedSupplier.class));
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onSuccess() on successSupplier")
        void executeRunnable_shouldCallOnSuccess_onSuccess() throws Throwable {
            halfOpenState.execute(() -> {});
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onException() when observable exceptionSupplier is thrown")
        void executeRunnable_shouldCallOnException_whenObservableExceptionThrown() {
            assertThatThrownBy(() -> halfOpenState.execute(() -> {
                throw new IllegalArgumentException();
            })).isInstanceOf(IllegalArgumentException.class);
            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onSuccess() when unobservable exceptionSupplier is thrown")
        void executeRunnable_shouldCallOnSuccess_whenUnobservableExceptionThrown() {
            assertThatThrownBy(() -> halfOpenState.execute(() -> {
                throw new IllegalStateException("Not observable");
            })).isInstanceOf(IllegalStateException.class);
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should return value and call strategy.onSuccess() on successSupplier")
        void executeSupplier_shouldReturnValueAndCallOnSuccess_onSuccess() throws Throwable {
            String result = halfOpenState.execute(() -> "successSupplier");
            assertThat(result).isEqualTo("successSupplier");
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should call strategy.onException() when observable exceptionSupplier is thrown")
        void executeSupplier_shouldCallOnException_whenObservableExceptionThrown() {
            assertThatThrownBy(() -> halfOpenState.execute((CheckedSupplier<String>) () -> {
                throw new IllegalArgumentException();
            })).isInstanceOf(IllegalArgumentException.class);
            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should call strategy.onSuccess() when unobservable exceptionSupplier is thrown")
        void executeSupplier_shouldCallOnSuccess_whenUnobservableExceptionThrown() {
            assertThatThrownBy(() -> halfOpenState.execute((CheckedSupplier<String>) () -> {
                throw new IllegalStateException("Not observable");
            })).isInstanceOf(IllegalStateException.class);
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: should switch to OPEN state when transition is TO_OPEN and trySetState succeeds")
        void shouldSwitchToOpenState_whenTransitionToOpenAndSetStateSucceeds() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(halfOpenState, openState)).thenReturn(true);
            halfOpenState.execute(() -> {});
            verify(circuitBreaker).trySetState(halfOpenState, openState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("UT: should switch to CLOSE state when transition is TO_CLOSE and trySetState succeeds")
        void shouldSwitchToCloseState_whenTransitionToCloseAndSetStateSucceeds() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
            when(circuitBreaker.trySetState(halfOpenState, closeState)).thenReturn(true);
            halfOpenState.execute(() -> {});
            verify(circuitBreaker).trySetState(halfOpenState, closeState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("UT: should NOT reset strategy when transition is TO_OPEN but trySetState fails")
        void shouldNotResetStrategy_whenTransitionToOpenButSetStateFails() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(halfOpenState, openState)).thenReturn(false);
            halfOpenState.execute(() -> {});
            verify(circuitBreaker).trySetState(halfOpenState, openState);
            verify(strategy, never()).reset();
        }

        @Test
        @DisplayName("UT: should NOT reset strategy when transition is TO_CLOSE but trySetState fails")
        void shouldNotResetStrategy_whenTransitionToCloseButSetStateFails() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
            when(circuitBreaker.trySetState(halfOpenState, closeState)).thenReturn(false);
            halfOpenState.execute(() -> {});
            verify(circuitBreaker).trySetState(halfOpenState, closeState);
            verify(strategy, never()).reset();
        }

        @Test
        @DisplayName("UT: should NOT attempt to switch state when transition is NO_TRANSITION")
        void shouldNotAttemptSwitchState_whenTransitionNoTransition() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.NO_TRANSITION);
            halfOpenState.execute(() -> {});
            verify(circuitBreaker, never()).trySetState(any(), any());
            verify(strategy, never()).reset();
        }
    }

    @Nested
    @DisplayName("Tests for setter logic")
    class SetterTests {

        @BeforeEach
        void setUp() {
            halfOpenState = new HalfOpenState(circuitBreaker, strategy, timer);
        }

        @Test
        @DisplayName("setCloseState should set state when not initialized")
        void setCloseState_shouldSetState_whenNotInitialized() {
            halfOpenState.setCloseState(closeState);
        }

        @Test
        @DisplayName("setCloseState should throw NullPointerException when state is null")
        void setCloseState_shouldThrowNPE_whenStateIsNull() {
            assertThatThrownBy(() -> halfOpenState.setCloseState(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("closeState cannot be null");
        }

        @Test
        @DisplayName("setCloseState should throw IllegalStateException when already initialized")
        void setCloseState_shouldThrowIllegalState_whenAlreadyInitialized() {
            halfOpenState.setCloseState(closeState);
            assertThatThrownBy(() -> halfOpenState.setCloseState(mock(CircuitState.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("cannot modify state with this method");
        }

        @Test
        @DisplayName("setOpenState should set state when not initialized")
        void setOpenState_shouldSetState_whenNotInitialized() {
            halfOpenState.setOpenState(openState);
            // No exceptionSupplier is a pass
        }

        @Test
        @DisplayName("setOpenState should throw NullPointerException when state is null")
        void setOpenState_shouldThrowNPE_whenStateIsNull() {
            assertThatThrownBy(() -> halfOpenState.setOpenState(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("openState cannot be null");
        }

        @Test
        @DisplayName("setOpenState should throw IllegalStateException when already initialized")
        void setOpenState_shouldThrowIllegalState_whenAlreadyInitialized() {
            halfOpenState.setOpenState(openState);
            assertThatThrownBy(() -> halfOpenState.setOpenState(mock(CircuitState.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("cannot modify state with this method");
        }
    }

    @Nested
    @DisplayName("UT: for RequestTimer interaction")
    class RequestTimerTests {

        @BeforeEach
        void setUp() {
            halfOpenState = new HalfOpenState(circuitBreaker, openState, closeState, strategy, timer);
        }

        @Test
        @DisplayName("should call onSuccess when timer executes runnable successfully")
        void shouldCallOnSuccess_whenTimerExecutesRunnableSuccessfully() throws Throwable {

            doAnswer(invocation -> {
                CheckedRunnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(timer).execute(any(CheckedRunnable.class));

            halfOpenState.execute(() -> {});

            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("should call onException when timer throws observable exception")
        void shouldCallOnException_whenTimerThrowsObservableException() throws Throwable {

            doThrow(new IllegalArgumentException("observable"))
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> halfOpenState.execute(() -> {}))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("should call onSuccess when timer throws unobservable exception")
        void shouldCallOnSuccess_whenTimerThrowsUnobservableException() throws Throwable {

            doThrow(new IllegalStateException("not observable"))
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> halfOpenState.execute(() -> {}))
                    .isInstanceOf(IllegalStateException.class);

            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("should propagate slow request exception from timer")
        void shouldPropagateSlowRequestException() throws Throwable {

            SlowRequestException slow = new SlowRequestException("slow");

            doThrow(slow).when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> halfOpenState.execute(() -> {}))
                    .isSameAs(slow);
        }

        @Test
        @DisplayName("should handle exception thrown after supplier execution")
        void shouldHandleTimerExceptionAfterSupplierExecution() throws Throwable {

            doAnswer(invocation -> {
                CheckedSupplier<String> supplier = invocation.getArgument(0);
                supplier.get();
                throw new IllegalArgumentException("slow request");
            }).when(timer).execute(any(CheckedSupplier.class));

            assertThatThrownBy(() -> halfOpenState.execute(() -> "value"))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(strategy).onException();
        }

        @Test
        @DisplayName("should still evaluate transition when timer throws exception")
        void shouldEvaluateTransitionWhenTimerThrows() throws Throwable {

            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(halfOpenState, openState)).thenReturn(true);

            doThrow(new IllegalArgumentException())
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> halfOpenState.execute(() -> {}))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(strategy).onException();
            verify(circuitBreaker).trySetState(halfOpenState, openState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("should transition to CLOSE when unobservable timer exception occurs")
        void shouldTransitionToClose_whenTimerThrowsUnobservableException() throws Throwable {

            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
            when(circuitBreaker.trySetState(halfOpenState, closeState)).thenReturn(true);

            doThrow(new IllegalStateException("not observable"))
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> halfOpenState.execute(() -> {}))
                    .isInstanceOf(IllegalStateException.class);

            verify(strategy).onSuccess();
            verify(circuitBreaker).trySetState(halfOpenState, closeState);
        }
    }
}
