package io.github.dmitriyiliyov.circuitbreaker.core.config;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlowRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CircuitBreakerConfigurationUnitTests {

    private final Consumer<CloseStateConfiguration.Builder> validCloseStateConsumer = builder -> builder
            .windowSize(100)
            .exceptionRateThreshold(0.5)
            .initialDelay(Duration.ZERO);

    private final Consumer<HalfOpenStateConfiguration.Builder> validHalfOpenStateConsumer = builder -> builder
            .type(HalfOpenType.NORMAL)
            .maxRequestInHalfOpenState(20)
            .maxExceptionCountInHalfOpenState(2);

    private CircuitBreakerConfiguration.Builder baseBuilder() {
        return CircuitBreakerConfiguration.builder()
                .name("test")
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("UT should throw exceptionSupplier when observableExceptions is null")
    public void shouldThrowExceptionWhenObservableExceptionsIsNull() {
        assertThatThrownBy(() -> baseBuilder()
                .observableExceptions(null)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("observableExceptions cannot be null");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when observableExceptions is empty")
    public void shouldThrowExceptionWhenObservableExceptionsIsEmpty() {
        assertThatThrownBy(() -> baseBuilder()
                .observableExceptions(Collections.emptySet())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observableExceptions cannot be empty");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when ignorableExceptions is null AND priority is IGNORABLE")
    public void shouldThrowExceptionWhenIgnorableExceptionsIsNullAndPriorityIsIgnorable() {
        assertThatThrownBy(() -> baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ignorableExceptions cannot be null or empty when priority 'IGNORABLE'");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when ignorableExceptions is empty AND priority is IGNORABLE")
    public void shouldThrowExceptionWhenIgnorableExceptionsIsEmptyAndPriorityIsIgnorable() {
        assertThatThrownBy(() -> baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Collections.emptySet())
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ignorableExceptions cannot be null or empty when priority 'IGNORABLE'");
    }

    @Test
    @DisplayName("should ALLOW null ignorableExceptions when priority is OBSERVABLE")
    public void shouldAllowNullIgnorableExceptionsWhenPriorityIsObservable() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .build();

        assertThat(config.getIgnorableExceptions()).isEmpty();
    }

    @Test
    @DisplayName("should ALLOW empty ignorableExceptions when priority is OBSERVABLE")
    public void shouldAllowEmptyIgnorableExceptionsWhenPriorityIsObservable() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Collections.emptySet())
                .exceptionPriority(ExceptionPriority.OBSERVABLE)
                .build();

        assertThat(config.getIgnorableExceptions()).isEmpty();
    }

    @Test
    @DisplayName("should ALLOW null ignorableExceptions when priority is null")
    public void shouldAllowNullIgnorableExceptionsWhenPriorityIsNull() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .build();

        assertThat(config.getIgnorableExceptions()).isEmpty();
    }

    @Test
    @DisplayName("UT: should throw when ignorableExceptions is null")
    void ignorableExceptions_shouldThrows_whenNullPassed() {
        assertThrows(NullPointerException.class, () ->
                baseBuilder()
                        .observableExceptions(Set.of(RuntimeException.class))
                        .ignorableExceptions(null)
                        .build()
        );
    }

    @Test
    @DisplayName("should ALLOW empty ignorableExceptions when priority is null")
    public void shouldAllowEmptyIgnorableExceptionsWhenPriorityIsNull() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Collections.emptySet())
                .exceptionPriority(ExceptionPriority.OBSERVABLE)
                .build();

        assertThat(config.getIgnorableExceptions()).isEmpty();
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters")
    public void shouldCreateConfigurationSuccessfullyWithValidParameters() {
        assertThatCode(() -> baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when closeState is null")
    public void shouldThrowIllegalArgumentExceptionWhenCloseStateIsNull() {
        assertThatThrownBy(() ->
                        CircuitBreakerConfiguration.builder()
                                .name("test")
                                .observableExceptions(Set.of(RuntimeException.class))
                                .ignorableExceptions(Set.of(IllegalStateException.class))
                                .exceptionPriority(ExceptionPriority.IGNORABLE)
                                .halfOpenState(validHalfOpenStateConsumer)
                                .waitDurationInOpenState(Duration.ofSeconds(30))
                                .build()
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("closeStateConfiguration cannot be null");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when waitDurationInOpenState is null")
    public void shouldThrowIllegalArgumentExceptionWhenWaitDurationInOpenStateIsNull() {
        assertThatThrownBy(() ->
                CircuitBreakerConfiguration.builder()
                        .name("test")
                        .observableExceptions(Set.of(RuntimeException.class))
                        .ignorableExceptions(Set.of(IllegalStateException.class))
                        .exceptionPriority(ExceptionPriority.IGNORABLE)
                        .closeState(validCloseStateConsumer)
                        .halfOpenState(validHalfOpenStateConsumer)
                        .build()
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("waitDurationInOpenState cannot be null");
    }

    @Test
    @DisplayName("should set isHalfOpenEnabled to false when halfOpenState is null")
    public void shouldSetIsHalfOpenEnabledFalseWhenHalfOpenStateIsNull() {
        CircuitBreakerConfiguration config = CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .closeState(validCloseStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();

        assertThat(config.getHalfOpenStateConfiguration().isHalfOpenStateEnabled()).isFalse();
    }

    @Test
    @DisplayName("should default lockFree to true when null")
    public void shouldDefaultLockFreeToTrueWhenNull() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .build();

        assertThat(config.getLockFree()).isTrue();
    }

    @Test
    @DisplayName("UT: should throw when lockFree is null")
    void lockFree_shouldThrows_whenNullPassed() {
        assertThrows(NullPointerException.class, () ->
                baseBuilder()
                        .observableExceptions(Set.of(RuntimeException.class))
                        .lockFree(null)
                        .build()
        );
    }

    @Test
    @DisplayName("should set lockFree to true when true")
    public void shouldSetLockFreeToTrueWhenTrue() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .lockFree(true)
                .build();

        assertThat(config.getLockFree()).isTrue();
    }

    @Test
    @DisplayName("should set lockFree to false when false")
    public void shouldSetLockFreeToFalseWhenFalse() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .lockFree(false)
                .build();

        assertThat(config.getLockFree()).isFalse();
    }

    @Test
    @DisplayName("should throw exceptionSupplier when ignorableExceptions is not empty but priority is null")
    public void shouldThrowExceptionWhenIgnorableExceptionsNotEmptyAndPriorityIsNull() {
        assertThatThrownBy(() -> baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("required exceptionPriority when ignorableExceptions not null or not empty");
    }

    @Test
    @DisplayName("prepareObservableAndIgnorableExceptions should prioritize OBSERVABLE exceptions")
    void prepareExceptions_shouldPrioritizeObservable() {
        Set<Class<? extends Throwable>> observable = new HashSet<>();
        observable.add(RuntimeException.class);
        observable.add(IllegalArgumentException.class);

        Set<Class<? extends Throwable>> ignorable = new HashSet<>();
        ignorable.add(RuntimeException.class);
        ignorable.add(IOException.class);

        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(observable)
                .ignorableExceptions(ignorable)
                .exceptionPriority(ExceptionPriority.OBSERVABLE)
                .lockFree(false)
                .build();

        // RuntimeException should remain in observable and be removed from ignorable
        assertThat(config.getObservableExceptions()).containsExactlyInAnyOrder(RuntimeException.class, IllegalArgumentException.class);
        assertThat(config.getIgnorableExceptions()).containsExactly(IOException.class);
    }

    @Test
    @DisplayName("prepareObservableAndIgnorableExceptions should prioritize IGNORABLE exceptions")
    void prepareExceptions_shouldPrioritizeIgnorable() {
        // Both sets contain RuntimeException
        Set<Class<? extends Throwable>> observable = new HashSet<>();
        observable.add(RuntimeException.class);
        observable.add(IllegalArgumentException.class);

        Set<Class<? extends Throwable>> ignorable = new HashSet<>();
        ignorable.add(RuntimeException.class);
        ignorable.add(IOException.class);

        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(observable)
                .ignorableExceptions(ignorable)
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .lockFree(false)
                .build();

        // RuntimeException should remain in ignorable and be removed from observable
        assertThat(config.getObservableExceptions()).containsExactly(IllegalArgumentException.class);
        assertThat(config.getIgnorableExceptions()).containsExactlyInAnyOrder(RuntimeException.class, IOException.class);
    }

    @Test
    @DisplayName("UT: isRequestTimerEnable should be true when maxRequestExecutionDuration is not null")
    void testRequestTimerEnabled() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .maxRequestExecutionDuration(Duration.ofSeconds(1))
                .build();

        assertThat(config.isRequestTimerEnable()).isTrue();
    }

    @Test
    @DisplayName("UT: isRequestTimerEnable should be false when maxRequestExecutionDuration is null")
    void testRequestTimerDisabled() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .build();

        assertThat(config.isRequestTimerEnable()).isFalse();
    }

    @Test
    @DisplayName("UT: should throw when maxRequestExecutionDuration is null")
    void maxRequestExecutionDuration_shouldThrows_whenNullPassed() {
        assertThrows(NullPointerException.class, () ->
                baseBuilder()
                        .observableExceptions(Set.of(RuntimeException.class))
                        .ignorableExceptions(Set.of(IllegalStateException.class))
                        .exceptionPriority(ExceptionPriority.IGNORABLE)
                        .maxRequestExecutionDuration(null)
                        .build()
        );
    }

    @Test
    @DisplayName("UT: SlowRequestException should be added to observable exceptions when timer is enabled")
    void testSlowRequestExceptionAddedWhenTimerEnabled() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(IllegalArgumentException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .maxRequestExecutionDuration(Duration.ofSeconds(1))
                .build();
        assertThat(config.getObservableExceptions()).contains(SlowRequestException.class, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT: SlowRequestException should not be added to observable exceptions when timer is disabled")
    void testSlowRequestExceptionNotAddedWhenTimerDisabled() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(IllegalArgumentException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .build();

        assertThat(config.getObservableExceptions()).doesNotContain(SlowRequestException.class);
        assertThat(config.getObservableExceptions()).contains(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT: SlowRequestException should not be added to observable exceptions when it is ignorable")
    void testSlowRequestExceptionNotAddedWhenIgnorable() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(IllegalArgumentException.class))
                .ignorableExceptions(Set.of(IllegalStateException.class, SlowRequestException.class))
                .exceptionPriority(ExceptionPriority.IGNORABLE)
                .maxRequestExecutionDuration(Duration.ofSeconds(1))
                .build();

        assertThat(config.getObservableExceptions()).doesNotContain(SlowRequestException.class);
        assertThat(config.getObservableExceptions()).contains(IllegalArgumentException.class);
        assertThat(config.getIgnorableExceptions()).contains(IllegalStateException.class, SlowRequestException.class);
    }
}