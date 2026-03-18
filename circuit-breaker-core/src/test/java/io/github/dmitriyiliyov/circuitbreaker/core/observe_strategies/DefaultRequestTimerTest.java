package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import io.github.dmitriyiliyov.circuitbreaker.core.CheckedRunnable;
import io.github.dmitriyiliyov.circuitbreaker.core.CheckedSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultRequestTimerTest {

    private static final Duration MAX_EXECUTION_TIME = Duration.ofMillis(100);
    private DefaultRequestTimer requestTimer;

    @BeforeEach
    void setUp() {
        requestTimer = new DefaultRequestTimer(MAX_EXECUTION_TIME);
    }

    @Nested
    @DisplayName("UT: when executing a CheckedRunnable")
    class CheckedRunnableTests {

        @Test
        @DisplayName("should execute the runnable")
        void shouldExecuteTheRunnable() throws Throwable {
            CheckedRunnable runnable = mock(CheckedRunnable.class);
            requestTimer.execute(runnable);
            verify(runnable).run();
        }

        @Test
        @DisplayName("should throw SlowRequestException if execution is too slow")
        void shouldThrowSlowRequestExceptionIfExecutionIsTooSlow() {
            assertThatThrownBy(() -> requestTimer.execute(() -> Thread.sleep(MAX_EXECUTION_TIME.toMillis() + 10)))
                    .isInstanceOf(SlowRequestException.class)
                    .hasMessage("The request was terminated because it was too slow");
        }

        @Test
        @DisplayName("should not throw exception if execution is within time limits")
        void shouldNotThrowExceptionIfExecutionIsWithinTimeLimits() throws Throwable {
            requestTimer.execute(() -> {});
        }
    }

    @Nested
    @DisplayName("UT: when executing a CheckedSupplier")
    class CheckedSupplierTests {

        @Test
        @DisplayName("should return the value from the supplier")
        void shouldReturnTheValueFromTheSupplier() throws Throwable {
            CheckedSupplier<String> supplier = () -> "result";
            String result = requestTimer.execute(supplier);
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("should throw SlowRequestException if execution is too slow")
        void shouldThrowSlowRequestExceptionIfExecutionIsTooSlow() {
            assertThatThrownBy(() -> requestTimer.execute((CheckedSupplier<String>) () -> {
                Thread.sleep(MAX_EXECUTION_TIME.toMillis() + 10);
                return "result";
            }))
                    .isInstanceOf(SlowRequestException.class)
                    .hasMessage("The request was terminated because it was too slow");
        }

        @Test
        @DisplayName("should return value if execution is within time limits")
        void shouldReturnValueIfExecutionIsWithinTimeLimits() throws Throwable {
            String result = requestTimer.execute(() -> "result");
            assertThat(result).isEqualTo("result");
        }
    }
}
