package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

public class CircuitBreakerConfigurationUnitTests {

    private final Consumer<CloseStateConfiguration.Builder> validCloseStateConsumer = builder -> builder
            .observeTime(Duration.ofSeconds(1))
            .exceptionRateThreshold(0.5)
            .initialDelay(Duration.ZERO);

    private CircuitBreakerConfiguration.Builder baseBuilder() {
        return CircuitBreakerConfiguration.builder()
                .name("test")
                .closeState(validCloseStateConsumer)
                .halfOpenStateEnabled(false)
                .waitDurationInOpenState(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("should throw exceptionSupplier when observableExceptions is null")
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
                .ignorableExceptions(null)
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
                .ignorableExceptions(null)
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
                .ignorableExceptions(null)
                .build();

        assertThat(config.getIgnorableExceptions()).isEmpty();
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
    @DisplayName("should throw exceptionSupplier when closeState is not configured")
    public void shouldThrowExceptionWhenCloseStateIsNotConfigured() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .halfOpenStateEnabled(false)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("closeState cannot be null");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when waitDurationInOpenState is not configured")
    public void shouldThrowExceptionWhenWaitDurationInOpenStateIsNotConfigured() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenStateEnabled(false)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("waitDurationInOpenState cannot be null");
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
    @DisplayName("should default lockFree to true when null")
    public void shouldDefaultLockFreeToTrueWhenNull() {
        CircuitBreakerConfiguration config = baseBuilder()
                .observableExceptions(Set.of(RuntimeException.class))
                .lockFree(null)
                .build();

        assertThat(config.getLockFree()).isTrue();
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
    @DisplayName("should throw exceptionSupplier when name is null")
    public void shouldThrowExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name(null)
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenStateEnabled(false)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name cannot be null, blank or empty");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when name is blank")
    public void shouldThrowExceptionWhenNameIsBlank() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name("   ")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenStateEnabled(false)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name cannot be null, blank or empty");
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
    @DisplayName("should throw exceptionSupplier when halfOpen enabled and maxRequestInHalfOpenState is null")
    public void shouldThrowWhenHalfOpenEnabledAndMaxRequestIsNull() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .halfOpenStateEnabled(true)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRequestInHalfOpenState cannot be null or == 0 when isHalfOpenStateEnabled == true");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when halfOpen enabled and maxRequestInHalfOpenState is zero")
    public void shouldThrowWhenHalfOpenEnabledAndMaxRequestIsZero() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRequestInHalfOpenState cannot be null or == 0 when isHalfOpenStateEnabled == true");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when halfOpen enabled and neither count nor rate provided")
    public void shouldThrowWhenHalfOpenEnabledAndNeitherCountNorRateProvided() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either maxExceptionCountInHalfOpenState or maxExceptionRateInHalfOpenState must be non null and > 0");
    }

    @Test
    @DisplayName("should calculate maxExceptionCount from rate when count not provided")
    public void shouldCalculateMaxExceptionCountFromRate() {
        CircuitBreakerConfiguration config = CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .maxExceptionRateInHalfOpenState(0.3)
                .build();

        assertThat(config.getMaxExceptionCountInHalfOpenState()).isEqualTo(3);
    }

    @Test
    @DisplayName("should use maxExceptionCount directly when provided")
    public void shouldUseMaxExceptionCountDirectlyWhenProvided() {
        CircuitBreakerConfiguration config = CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(4)
                .build();

        assertThat(config.getMaxExceptionCountInHalfOpenState()).isEqualTo(4);
        assertThat(config.getMaxRequestInHalfOpenState()).isEqualTo(10);
        assertThat(config.isHalfOpenStateEnabled()).isTrue();
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
}