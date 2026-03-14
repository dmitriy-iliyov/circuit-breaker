package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CloseStateConfigurationUnitTests {

    @Test
    @DisplayName("should throw exceptionSupplier when neither observeTime nor windowSize is provided")
    public void shouldThrowExceptionWhenNeitherObserveTimeNorWindowSizeIsProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .exceptionRateThreshold(0.5)
                .initialDelay(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either observeTime or windowSize must be provided");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when both observeTime and windowSize are provided")
    public void shouldThrowExceptionWhenBothObserveTimeAndWindowSizeAreProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .windowSize(10)
                .exceptionRateThreshold(0.5)
                .initialDelay(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both observeTime and windowSize cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when neither exceptionRateThreshold nor exceptionCountThreshold is provided")
    public void shouldThrowExceptionWhenNeitherExceptionRateThresholdNorExceptionCountThresholdIsProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowSize(100)
                .initialDelay(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either exceptionCountThreshold or exceptionRateThreshold must be non null and >= 0");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when both exceptionRateThreshold and exceptionCountThreshold are provided")
    public void shouldThrowExceptionWhenBothExceptionRateThresholdAndExceptionCountThresholdAreProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowSize(100)
                .exceptionRateThreshold(0.5)
                .exceptionCountThreshold(5)
                .initialDelay(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both exceptionRateThreshold and exceptionCountThreshold cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should throw exceptionSupplier when initialDelay is null for FIXED window")
    public void shouldThrowExceptionWhenInitialDelayIsNullForFixedWindow() {
        CloseStateConfiguration closeStateConfiguration = CloseStateConfiguration.builder()
                .windowSize(100)
                .exceptionRateThreshold(0.5)
                .build();

        assertEquals(Duration.ZERO, closeStateConfiguration.getInitialDelay());
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Time + Rate)")
    public void shouldCreateConfigurationSuccessfullyWithTimeAndRate() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .initialDelay(Duration.ZERO)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Size + Count)")
    public void shouldCreateConfigurationSuccessfullyWithSizeAndCount() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowSize(10)
                .exceptionCountThreshold(5)
                .initialDelay(Duration.ZERO)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Size + Rate)")
    public void shouldCreateConfigurationSuccessfullyWithSizeAndRate() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowSize(10)
                .exceptionRateThreshold(0.5)
                .initialDelay(Duration.ZERO)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully for MOVING window without initialDelay")
    public void shouldCreateConfigurationSuccessfullyForMovingWindowWithoutInitialDelay() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .build())
                .doesNotThrowAnyException();
    }
}
