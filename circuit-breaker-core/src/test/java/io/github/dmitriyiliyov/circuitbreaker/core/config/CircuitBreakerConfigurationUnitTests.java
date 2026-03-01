package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

public class CircuitBreakerConfigurationUnitTests {

    private final Consumer<CloseStateConfiguration.Builder> validCloseStateConsumer = builder -> builder
            .windowMoveType(WindowType.FIXED)
            .observeTime(Duration.ofSeconds(1))
            .exceptionRateThreshold(0.5)
            .observeStartTime(Duration.ZERO);

    private final Consumer<HalfOpenStateConfiguration.Builder> validHalfOpenStateConsumer = builder -> builder
            .observeTime(Duration.ofSeconds(1))
            .exceptionRateThreshold(0.5);

    private final Consumer<OpenStateConfiguration.Builder> validOpenStateConsumer = builder -> builder
            .observeTime(Duration.ofSeconds(1));

    @Test
    @DisplayName("should throw exception when observableExceptions is null")
    public void shouldThrowExceptionWhenObservableExceptionsIsNull() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(null)
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observableExceptions cannot be null or empty");
    }

    @Test
    @DisplayName("should throw exception when observableExceptions is empty")
    public void shouldThrowExceptionWhenObservableExceptionsIsEmpty() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(Collections.emptySet())
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observableExceptions cannot be null or empty");
    }

    @Test
    @DisplayName("should throw exception when ignorableExceptions is null")
    public void shouldThrowExceptionWhenIgnorableExceptionsIsNull() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(null)
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ignorableExceptions cannot be null or empty");
    }

    @Test
    @DisplayName("should throw exception when ignorableExceptions is empty")
    public void shouldThrowExceptionWhenIgnorableExceptionsIsEmpty() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Collections.emptySet())
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ignorableExceptions cannot be null or empty");
    }

    @Test
    @DisplayName("should throw exception when closeState is not configured")
    public void shouldThrowExceptionWhenCloseStateIsNotConfigured() {
        // Since we cannot pass null to closeState(Consumer), we simulate missing configuration by not calling it.
        // But the builder initializes fields to null.
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(RuntimeException.class))
                // .closeState(...) omitted
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("closeState cannot be null");
    }

    @Test
    @DisplayName("should throw exception when openState is not configured")
    public void shouldThrowExceptionWhenOpenStateIsNotConfigured() {
        assertThatThrownBy(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                // .openState(...) omitted
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("openState cannot be null");
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters")
    public void shouldCreateConfigurationSuccessfullyWithValidParameters() {
        assertThatCode(() -> CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should default lockFree to true when null")
    public void shouldDefaultLockFreeToTrueWhenNull() {
        CircuitBreakerConfiguration config = CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .lockFree(null)
                .build();

        assertThat(config.getLockFree()).isTrue();
    }

    @Test
    @DisplayName("should set lockFree to true when true")
    public void shouldSetLockFreeToTrueWhenTrue() {
        CircuitBreakerConfiguration config = CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .lockFree(true)
                .build();

        assertThat(config.getLockFree()).isTrue();
    }

    @Test
    @DisplayName("should set lockFree to false when false")
    public void shouldSetLockFreeToFalseWhenFalse() {
        CircuitBreakerConfiguration config = CircuitBreakerConfiguration.builder()
                .observableExceptions(Set.of(RuntimeException.class))
                .ignorableExceptions(Set.of(RuntimeException.class))
                .closeState(validCloseStateConsumer)
                .halfOpenState(validHalfOpenStateConsumer)
                .openState(validOpenStateConsumer)
                .lockFree(false)
                .build();

        assertThat(config.getLockFree()).isFalse();
    }
}
