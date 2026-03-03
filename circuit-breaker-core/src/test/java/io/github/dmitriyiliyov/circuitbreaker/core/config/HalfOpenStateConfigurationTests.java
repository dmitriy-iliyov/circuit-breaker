package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HalfOpenStateConfigurationTests {

    @Test
    @DisplayName("should throw exception when neither observeTime nor windowSize is provided")
    public void shouldThrowExceptionWhenNeitherObserveTimeNorWindowSizeIsProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .exceptionRateThreshold(0.5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either observeTime or windowSize must be provided");
    }

    @Test
    @DisplayName("should throw exception when both observeTime and windowSize are provided")
    public void shouldThrowExceptionWhenBothObserveTimeAndWindowSizeAreProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .windowSize(10)
                .exceptionRateThreshold(0.5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both observeTime and windowSize cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should throw exception when neither exceptionRateThreshold nor exceptionCountThreshold is provided")
    public void shouldThrowExceptionWhenNeitherExceptionRateThresholdNorExceptionCountThresholdIsProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either exceptionRateThreshold or exceptionCountThreshold must be provided");
    }

    @Test
    @DisplayName("should throw exception when both exceptionRateThreshold and exceptionCountThreshold are provided")
    public void shouldThrowExceptionWhenBothExceptionRateThresholdAndExceptionCountThresholdAreProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .exceptionCountThreshold(5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both exceptionRateThreshold and exceptionCountThreshold cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Time + Rate)")
    public void shouldCreateConfigurationSuccessfullyWithTimeAndRate() {
        assertThatCode(() -> HalfOpenStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .exceptionRateThreshold(0.5)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully with valid parameters (Size + Count)")
    public void shouldCreateConfigurationSuccessfullyWithSizeAndCount() {
        assertThatCode(() -> HalfOpenStateConfiguration.builder()
                .windowSize(10)
                .exceptionCountThreshold(5)
                .build())
                .doesNotThrowAnyException();
    }
}
