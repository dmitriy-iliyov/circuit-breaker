package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CloseStateConfigurationUnitTests {

    @Test
    @DisplayName("should throw exception when windowType is null")
    public void shouldThrowExceptionWhenWindowTypeIsNull() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .observeStartTime(Duration.ZERO)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("windowType cannot be null");
    }

    @Test
    @DisplayName("should throw exception when neither observeTime nor windowSize is provided")
    public void shouldThrowExceptionWhenNeitherObserveTimeNorWindowSizeIsProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .exceptionRateThreshold(0.5)
                .observeStartTime(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either observeTime or windowSize must be provided");
    }

    @Test
    @DisplayName("should throw exception when both observeTime and windowSize are provided")
    public void shouldThrowExceptionWhenBothObserveTimeAndWindowSizeAreProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .observeTime(Duration.ofSeconds(1))
                .windowSize(10)
                .exceptionRateThreshold(0.5)
                .observeStartTime(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both observeTime and windowSize cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should throw exception when neither exceptionRateThreshold nor exceptionCountThreshold is provided")
    public void shouldThrowExceptionWhenNeitherExceptionRateThresholdNorExceptionCountThresholdIsProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .observeTime(Duration.ofSeconds(1))
                .observeStartTime(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either exceptionRateThreshold or exceptionCountThreshold must be provided");
    }

    @Test
    @DisplayName("should throw exception when both exceptionRateThreshold and exceptionCountThreshold are provided")
    public void shouldThrowExceptionWhenBothExceptionRateThresholdAndExceptionCountThresholdAreProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .exceptionCountThreshold(5)
                .observeStartTime(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both exceptionRateThreshold and exceptionCountThreshold cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should throw exception when waitTimeBeforeStart is null for FIXED window")
    public void shouldThrowExceptionWhenWaitTimeBeforeStartIsNullForFixedWindow() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("waitTimeBeforeStart cannot be null");
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Time + Rate)")
    public void shouldCreateConfigurationSuccessfullyWithTimeAndRate() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .observeStartTime(Duration.ZERO)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Size + Count)")
    public void shouldCreateConfigurationSuccessfullyWithSizeAndCount() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.FIXED)
                .windowSize(10)
                .exceptionCountThreshold(5)
                .observeStartTime(Duration.ZERO)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully for MOVING window without waitTimeBeforeStart")
    public void shouldCreateConfigurationSuccessfullyForMovingWindowWithoutWaitTimeBeforeStart() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowMoveType(WindowType.MOVING)
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                // observeStartTime is not set, so it's null. Should be allowed for MOVING.
                .build())
                .doesNotThrowAnyException();
    }
}
