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

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GradualHalfOpenStateUnitTests {

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

    private GradualHalfOpenState gradualState;
    private final float multiplier = 2.0f;

    @BeforeEach
    void setUp() {
        when(circuitBreaker.getChecker()).thenReturn(throwable -> throwable instanceof IllegalArgumentException);
        gradualState = new GradualHalfOpenState(circuitBreaker, openState, closeState, strategy, timer, multiplier);
    }

    private void forcePercentTo(int percent) throws Exception {
        Field field = GradualHalfOpenState.class.getDeclaredField("percentToLet");
        field.setAccessible(true);
        ((AtomicInteger) field.get(gradualState)).set(percent);
    }

    @Nested
    @DisplayName("Tests for Gradual Sampling and Percent Progression")
    class SamplingAndGradualProgressionTests {

        @BeforeEach
        void setUpTimer() throws Throwable {
            lenient().doAnswer(invocation -> {
                invocation.<CheckedRunnable>getArgument(0).run();
                return null;
            }).when(timer).execute(any(CheckedRunnable.class));

            lenient().doAnswer(invocation ->
                    invocation.<CheckedSupplier<?>>getArgument(0).get()
            ).when(timer).execute(any(CheckedSupplier.class));

            when(strategy.getTransition()).thenReturn(HalfOpenTransition.NO_TRANSITION);
        }

        @Test
        @DisplayName("UT: should refuse runnable requests with GradualHalfOpenRefuseException until threshold is met")
        void shouldRefuseRunnableRequests_untilThresholdMet() throws Throwable {
            for (int i = 0; i < 9; i++) {
                assertThatThrownBy(() -> gradualState.execute(() -> {}))
                        .isInstanceOf(GradualHalfOpenRefuseException.class);
            }
            gradualState.execute(() -> {});
            verify(timer, times(1)).execute(any(CheckedRunnable.class));
        }

        @Test
        @DisplayName("UT: should refuse supplier requests with GradualHalfOpenRefuseException until threshold is met")
        void shouldRefuseSupplierRequests_untilThresholdMet() throws Throwable {
            for (int i = 0; i < 9; i++) {
                assertThatThrownBy(() -> gradualState.execute((CheckedSupplier<String>) () -> "result"))
                        .isInstanceOf(GradualHalfOpenRefuseException.class);
            }
            String result = gradualState.execute(() -> "result");
            assertThat(result).isEqualTo("result");
            verify(timer, times(1)).execute(any(CheckedSupplier.class));
        }

        @Test
        @DisplayName("UT: should increase percent and NOT set state when transition is TO_CLOSE and percent < 100")
        void shouldIncreasePercent_whenTransitionToCloseAndUnder100() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);

            for (int i = 0; i < 9; i++) {
                try { gradualState.execute(() -> {}); } catch (GradualHalfOpenRefuseException ignored) {}
            }
            gradualState.execute(() -> {});

            verify(circuitBreaker, never()).trySetState(any(), any());
            verify(strategy).reset();

            when(strategy.getTransition()).thenReturn(HalfOpenTransition.NO_TRANSITION);
            for (int i = 0; i < 4; i++) {
                assertThatThrownBy(() -> gradualState.execute(() -> {}))
                        .isInstanceOf(GradualHalfOpenRefuseException.class);
            }
            gradualState.execute(() -> {});
            verify(timer, times(2)).execute(any(CheckedRunnable.class));
        }

        @Test
        @DisplayName("UT: should reset percent to 10 when transition is TO_OPEN")
        void shouldResetPercent_whenTransitionToOpen() throws Throwable {
            forcePercentTo(100);

            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(gradualState, openState)).thenReturn(true);

            gradualState.execute(() -> {});

            verify(circuitBreaker).trySetState(gradualState, openState);
            verify(strategy).reset();

            for (int i = 0; i < 9; i++) {
                assertThatThrownBy(() -> gradualState.execute(() -> {}))
                        .isInstanceOf(GradualHalfOpenRefuseException.class);
            }
        }
    }

    @Nested
    @DisplayName("Tests for execute and transition logic")
    class ExecuteAndTransitionTests {

        @BeforeEach
        void setUp() throws Throwable {
            forcePercentTo(100);

            lenient().doAnswer(invocation -> {
                invocation.<CheckedRunnable>getArgument(0).run();
                return null;
            }).when(timer).execute(any(CheckedRunnable.class));

            lenient().doAnswer(invocation ->
                    invocation.<CheckedSupplier<?>>getArgument(0).get()
            ).when(timer).execute(any(CheckedSupplier.class));
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onSuccess() on success")
        void executeRunnable_shouldCallOnSuccess_onSuccess() throws Throwable {
            gradualState.execute(() -> {});
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onException() when observable exception is thrown")
        void executeRunnable_shouldCallOnException_whenObservableExceptionThrown() {
            assertThatThrownBy(() -> gradualState.execute(() -> {
                throw new IllegalArgumentException();
            })).isInstanceOf(IllegalArgumentException.class);
            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("UT: execute(CheckedRunnable) should call strategy.onSuccess() when unobservable exception is thrown")
        void executeRunnable_shouldCallOnSuccess_whenUnobservableExceptionThrown() {
            assertThatThrownBy(() -> gradualState.execute(() -> {
                throw new IllegalStateException("Not observable");
            })).isInstanceOf(IllegalStateException.class);
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should return value and call strategy.onSuccess() on success")
        void executeSupplier_shouldReturnValueAndCallOnSuccess_onSuccess() throws Throwable {
            String result = gradualState.execute(() -> "successSupplier");
            assertThat(result).isEqualTo("successSupplier");
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should call strategy.onException() when observable exception is thrown")
        void executeSupplier_shouldCallOnException_whenObservableExceptionThrown() {
            assertThatThrownBy(() -> gradualState.execute((CheckedSupplier<String>) () -> {
                throw new IllegalArgumentException();
            })).isInstanceOf(IllegalArgumentException.class);
            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("UT: execute(CheckedSupplier) should call strategy.onSuccess() when unobservable exception is thrown")
        void executeSupplier_shouldCallOnSuccess_whenUnobservableExceptionThrown() {
            assertThatThrownBy(() -> gradualState.execute((CheckedSupplier<String>) () -> {
                throw new IllegalStateException("Not observable");
            })).isInstanceOf(IllegalStateException.class);
            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("UT: should switch to OPEN state when transition is TO_OPEN and trySetState succeeds")
        void shouldSwitchToOpenState_whenTransitionToOpenAndSetStateSucceeds() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(gradualState, openState)).thenReturn(true);
            gradualState.execute(() -> {});
            verify(circuitBreaker).trySetState(gradualState, openState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("UT: should switch to CLOSE state when transition is TO_CLOSE, percent >= 100, and trySetState succeeds")
        void shouldSwitchToCloseState_whenTransitionToCloseAndSetStateSucceeds() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
            when(circuitBreaker.trySetState(gradualState, closeState)).thenReturn(true);
            gradualState.execute(() -> {});
            verify(circuitBreaker).trySetState(gradualState, closeState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("UT: should NOT reset strategy when transition is TO_OPEN but trySetState fails")
        void shouldNotResetStrategy_whenTransitionToOpenButSetStateFails() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(gradualState, openState)).thenReturn(false);
            gradualState.execute(() -> {});
            verify(circuitBreaker).trySetState(gradualState, openState);
            verify(strategy, never()).reset();
        }

        @Test
        @DisplayName("UT: should NOT reset strategy when transition is TO_CLOSE (percent >= 100) but trySetState fails")
        void shouldNotResetStrategy_whenTransitionToCloseButSetStateFails() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
            when(circuitBreaker.trySetState(gradualState, closeState)).thenReturn(false);
            gradualState.execute(() -> {});
            verify(circuitBreaker).trySetState(gradualState, closeState);
            verify(strategy, never()).reset();
        }

        @Test
        @DisplayName("UT: should NOT attempt to switch state when transition is NO_TRANSITION")
        void shouldNotAttemptSwitchState_whenTransitionNoTransition() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.NO_TRANSITION);
            gradualState.execute(() -> {});
            verify(circuitBreaker, never()).trySetState(any(), any());
            verify(strategy, never()).reset();
        }

        @Test
        @DisplayName("UT: should NOT reset strategy when transition is TO_CLOSE and compareAndSet fails")
        void shouldNotResetStrategy_whenTransitionToCloseAndCompareAndSetFails() throws Throwable {
            AtomicInteger spiedPercent = spy(new AtomicInteger(10));
            doReturn(false).when(spiedPercent).compareAndSet(anyInt(), anyInt());

            Field percentField = GradualHalfOpenState.class.getDeclaredField("percentToLet");
            percentField.setAccessible(true);
            percentField.set(gradualState, spiedPercent);

            Field countField = GradualHalfOpenState.class.getDeclaredField("requestCount");
            countField.setAccessible(true);
            ((AtomicInteger) countField.get(gradualState)).set(9);

            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);

            gradualState.execute(() -> {});

            verify(strategy, never()).reset();
        }
    }

    @Nested
    @DisplayName("Tests for setter logic")
    class SetterTests {

        @BeforeEach
        void setUp() {
            gradualState = new GradualHalfOpenState(circuitBreaker, strategy, timer, multiplier);
        }

        @Test
        @DisplayName("setCloseState should set state when not initialized")
        void setCloseState_shouldSetState_whenNotInitialized() {
            gradualState.setCloseState(closeState);
        }

        @Test
        @DisplayName("setCloseState should throw NullPointerException when state is null")
        void setCloseState_shouldThrowNPE_whenStateIsNull() {
            assertThatThrownBy(() -> gradualState.setCloseState(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("closeState cannot be null");
        }

        @Test
        @DisplayName("setCloseState should throw IllegalStateException when already initialized")
        void setCloseState_shouldThrowIllegalState_whenAlreadyInitialized() {
            gradualState.setCloseState(closeState);
            assertThatThrownBy(() -> gradualState.setCloseState(mock(CircuitState.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("cannot modify state with this method");
        }

        @Test
        @DisplayName("setOpenState should set state when not initialized")
        void setOpenState_shouldSetState_whenNotInitialized() {
            gradualState.setOpenState(openState);
        }

        @Test
        @DisplayName("setOpenState should throw NullPointerException when state is null")
        void setOpenState_shouldThrowNPE_whenStateIsNull() {
            assertThatThrownBy(() -> gradualState.setOpenState(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("openState cannot be null");
        }

        @Test
        @DisplayName("setOpenState should throw IllegalStateException when already initialized")
        void setOpenState_shouldThrowIllegalState_whenAlreadyInitialized() {
            gradualState.setOpenState(openState);
            assertThatThrownBy(() -> gradualState.setOpenState(mock(CircuitState.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("cannot modify state with this method");
        }
    }

    @Nested
    @DisplayName("UT: for RequestTimer interaction")
    class RequestTimerTests {

        @BeforeEach
        void setUp() throws Exception {
            forcePercentTo(100);
        }

        @Test
        @DisplayName("should call onSuccess when timer executes runnable successfully")
        void shouldCallOnSuccess_whenTimerExecutesRunnableSuccessfully() throws Throwable {
            doAnswer(invocation -> {
                CheckedRunnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(timer).execute(any(CheckedRunnable.class));

            gradualState.execute(() -> {});

            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("should call onException when timer throws observable exception")
        void shouldCallOnException_whenTimerThrowsObservableException() throws Throwable {
            doThrow(new IllegalArgumentException("observable"))
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> gradualState.execute(() -> {}))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(strategy).onException();
            verify(strategy, never()).onSuccess();
        }

        @Test
        @DisplayName("should call onSuccess when timer throws unobservable exception")
        void shouldCallOnSuccess_whenTimerThrowsUnobservableException() throws Throwable {
            doThrow(new IllegalStateException("not observable"))
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> gradualState.execute(() -> {}))
                    .isInstanceOf(IllegalStateException.class);

            verify(strategy).onSuccess();
            verify(strategy, never()).onException();
        }

        @Test
        @DisplayName("should propagate slow request exception from timer")
        void shouldPropagateSlowRequestException() throws Throwable {
            SlowRequestException slow = new SlowRequestException("slow");
            doThrow(slow).when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> gradualState.execute(() -> {}))
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

            assertThatThrownBy(() -> gradualState.execute(() -> "value"))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(strategy).onException();
        }

        @Test
        @DisplayName("should still evaluate transition when timer throws exception")
        void shouldEvaluateTransitionWhenTimerThrows() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_OPEN);
            when(circuitBreaker.trySetState(gradualState, openState)).thenReturn(true);

            doThrow(new IllegalArgumentException())
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> gradualState.execute(() -> {}))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(strategy).onException();
            verify(circuitBreaker).trySetState(gradualState, openState);
            verify(strategy).reset();
        }

        @Test
        @DisplayName("should transition to CLOSE when unobservable timer exception occurs and percent is 100")
        void shouldTransitionToClose_whenTimerThrowsUnobservableException() throws Throwable {
            when(strategy.getTransition()).thenReturn(HalfOpenTransition.TO_CLOSE);
            when(circuitBreaker.trySetState(gradualState, closeState)).thenReturn(true);

            doThrow(new IllegalStateException("not observable"))
                    .when(timer).execute(any(CheckedRunnable.class));

            assertThatThrownBy(() -> gradualState.execute(() -> {}))
                    .isInstanceOf(IllegalStateException.class);

            verify(strategy).onSuccess();
            verify(circuitBreaker).trySetState(gradualState, closeState);
        }
    }
}