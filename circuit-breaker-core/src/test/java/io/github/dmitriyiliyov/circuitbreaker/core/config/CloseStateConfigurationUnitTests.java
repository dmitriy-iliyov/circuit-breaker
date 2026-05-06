package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CloseStateConfigurationUnitTests {

    @Test
    @DisplayName("UT should throw NullPointerException when windowSize is not set before build")
    public void shouldThrowExceptionWhenWindowSizeIsNotProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .exceptionRateThreshold(0.5)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("windowSize cannot be null");
    }

    @Test
    @DisplayName("UT should throw NullPointerException when windowSize is explicitly set to null")
    public void shouldThrowExceptionWhenWindowSizeIsNull() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().windowSize(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("windowSize cannot be null");
    }

    @Test
    @DisplayName("UT should throw IllegalArgumentException when windowSize is zero")
    public void shouldThrowExceptionWhenWindowSizeIsZero() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().windowSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize cannot be <= 0");
    }

    @Test
    @DisplayName("UT should throw IllegalArgumentException when windowSize is negative")
    public void shouldThrowExceptionWhenWindowSizeIsNegative() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().windowSize(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize cannot be <= 0");
    }

    @Test
    @DisplayName("UT should throw NullPointerException when exceptionRateThreshold is explicitly set to null")
    public void shouldThrowExceptionWhenExceptionRateThresholdIsNull() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().exceptionRateThreshold(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("exceptionRateThreshold cannot be null");
    }

    @Test
    @DisplayName("UT should throw IllegalArgumentException when exceptionRateThreshold is negative")
    public void shouldThrowExceptionWhenExceptionRateThresholdIsNegative() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().exceptionRateThreshold(-0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionRateThreshold cannot be < 0");
    }

    @Test
    @DisplayName("UT should throw NullPointerException when exceptionCountThreshold is explicitly set to null")
    public void shouldThrowExceptionWhenExceptionCountThresholdIsNull() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().exceptionCountThreshold(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("exceptionCountThreshold cannot be null");
    }

    @Test
    @DisplayName("UT should throw IllegalArgumentException when exceptionCountThreshold is negative")
    public void shouldThrowExceptionWhenExceptionCountThresholdIsNegative() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().exceptionCountThreshold(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionCountThreshold cannot be < 0");
    }

    @Test
    @DisplayName("UT should throw NullPointerException when initialDelay is explicitly set to null")
    public void shouldThrowExceptionWhenInitialDelayIsNull() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder().initialDelay(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("initialDelay cannot be null");
    }

    @Test
    @DisplayName("UT should throw IllegalArgumentException when neither exceptionRateThreshold nor exceptionCountThreshold is provided")
    public void shouldThrowExceptionWhenNeitherThresholdIsProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowSize(100)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either exceptionCountThreshold or exceptionRateThreshold must be non null and >= 0");
    }

    @Test
    @DisplayName("UT should throw IllegalArgumentException when both exceptionRateThreshold and exceptionCountThreshold are provided")
    public void shouldThrowExceptionWhenBothThresholdsAreProvided() {
        assertThatThrownBy(() -> CloseStateConfiguration.builder()
                .windowSize(100)
                .exceptionRateThreshold(0.5)
                .exceptionCountThreshold(5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both exceptionRateThreshold and exceptionCountThreshold cannot be provided simultaneously");
    }

    @Test
    @DisplayName("UT should default initialDelay to Duration.ZERO when not provided")
    public void shouldDefaultInitialDelayToZeroWhenNotProvided() {
        CloseStateConfiguration config = CloseStateConfiguration.builder()
                .windowSize(100)
                .exceptionRateThreshold(0.5)
                .build();

        assertEquals(Duration.ZERO, config.getInitialDelay());
    }

    @Test
    @DisplayName("UT should create configuration successfully and apply Math.ceil for exception count based on rate")
    public void shouldCreateConfigurationSuccessfullyAndCeilRate() {
        CloseStateConfiguration config = CloseStateConfiguration.builder()
                .windowSize(10)
                .exceptionRateThreshold(0.25)
                .build();

        assertEquals(10, config.getWindowSize());
        assertEquals(3, config.getExceptionCountThreshold());
    }

    @Test
    @DisplayName("UT should create configuration successfully with Size and Count")
    public void shouldCreateConfigurationSuccessfullyWithSizeAndCount() {
        CloseStateConfiguration config = CloseStateConfiguration.builder()
                .windowSize(10)
                .exceptionCountThreshold(5)
                .initialDelay(Duration.ofSeconds(2))
                .build();

        assertEquals(10, config.getWindowSize());
        assertEquals(5, config.getExceptionCountThreshold());
        assertEquals(Duration.ofSeconds(2), config.getInitialDelay());
    }

    @Test
    @DisplayName("UT should allow zero for exceptionCountThreshold")
    public void shouldAllowZeroForExceptionCountThreshold() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowSize(10)
                .exceptionCountThreshold(0)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT should allow zero for exceptionRateThreshold")
    public void shouldAllowZeroForExceptionRateThreshold() {
        assertThatCode(() -> CloseStateConfiguration.builder()
                .windowSize(10)
                .exceptionRateThreshold(0.0)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT should return correct toString representation")
    public void shouldReturnCorrectToStringRepresentation() {
        CloseStateConfiguration config = CloseStateConfiguration.builder()
                .windowSize(50)
                .exceptionCountThreshold(15)
                .initialDelay(Duration.ofSeconds(5))
                .build();

        String toString = config.toString();

        assertTrue(toString.startsWith("CloseStateConfiguration{"));
        assertTrue(toString.contains("windowSize=50"));
        assertTrue(toString.contains("exceptionCountThreshold=15"));
        assertTrue(toString.contains("initialDelay=PT5S"));
        assertTrue(toString.endsWith("}"));
    }
}